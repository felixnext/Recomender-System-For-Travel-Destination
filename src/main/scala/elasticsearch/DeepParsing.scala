package elasticsearch

import scala.util.Try
import scala.util.matching.Regex
import scala.language.implicitConversions

import tools.Math.{fahrenheitToCelsiusConverter => conv, std}

import elasticsearch.ElasticsearchClient._

import breeze.stats.distributions.Gaussian

import org.apache.commons.lang.StringEscapeUtils.{escapeJava => escape}

/**
 * Parses the query and extracts mentioned countries, languages and temperature
 * thus elastic query could be related w.r.t mentioned information.
 */
object DeepParsing {


  /**
   * Creates elastic query consider mentions of temperature, language and country within raw text
   * @param text Full text query.
   * @return Elastic query
   */
  def apply(text: String, topK: Int = 10, from: Int = 0): List[ElasticLocationDoc] = {
    val extraction = parseQuery(text)

    val country =  extraction.countries.map(country =>
      s"""
         |{
         |"match":{
         |   "country":{
         |      "query":"$country",
         |      "boost":2
         |   }
         | }
         |},
       """.stripMargin).mkString("\n")

    val language = extraction.languages.map(lang =>
      s"""
         |{
         |  "match":{
         |    "language":{
         |      "query":"$lang",
         |      "boost":2
         |    }
         |  }
         |},
       """.stripMargin).mkString("\n")


    val window = topK * 10
    val escapedText = escape(text)
    val jsonQuery =
      s"""
         |{
         |  "from":$from,
         |  "size":$topK,
         |  "query":{
         |    "match":{
         |      "paragraph_texts":{
         |        "query":"$escapedText",
         |        "minimum_should_match":"30%"
         |      }
         |    }
         |  },
         |  "rescore":{
         |    "window_size":$window,
         |    "query":{
         |      "rescore_query":{
         |        "bool":{
         |          "should":[
         |          $country
         |          $language
         |            {
         |              "match_phrase":{
         |                "paragraph_texts":{
         |                  "query":"$escapedText",
         |                  "slop":50,
         |                  "boost":3
         |                }
         |              }
         |            }
         |          ]
         |        }
         |      }
         |    }
         |  }
         |}
         |
       """.stripMargin

    implicit def StringToDouble(s: Option[String]): Option[Double] = Try(Some(s.get.toDouble)).getOrElse(None)

    type TemperatureRange = (Option[Double],Option[Double],Option[Double])

    //universal temperature mapping used in kb
    val tempDist: Map[String,String] => String => TemperatureRange = clim => month => {
      val mean = clim.get(month+"MeanC")
      val max = clim.get(month+ "HighC")
      val min = clim.get(month+ "LowC")
      (min,mean,max)
    }

    //elastic search request
    val locations  = indices.flatMap(index => parseLocationResult(request(jsonQuery, index)))

    //compute boost factor w.r.t temperature
    val boostFactor: TemperatureRange => Extraction => Option[Double] = t => extraction => {
      t match {
        case (Some(min), Some(mean), Some(max)) =>
          val sigma = std(Vector(min.toDouble,max.toDouble))(mean.toDouble)
          val dist = new Gaussian(mean,sigma)

          val tempInText = if (extraction.range.size > 0) extraction.range
          else if(extraction.temperature.size > 0) extraction.temperature.map(t => (t,t))
          else Seq()
          if(tempInText.size > 0)
            Some(1.0 + tempInText.flatMap(tempRange => Seq(dist(tempRange._1), dist(tempRange._2))).max * dist.normalizer)
          else None
        case _ => None
      }
    }

    //boost found locations
    val boostLocation: Map[String,String] => String => ElasticLocationDoc => Unit = clim => month => location => {
      val t = tempDist(clim)(month)
      val factor = boostFactor(t)(extraction)
      if(factor.isDefined && location.score.isDefined) location.score = Some(location.score.get * factor.get)
    }

    //boost rsult if temperature of found location match a temperature mentioned in the query
    locations.foreach{ location =>
      val clim = location.climate.getOrElse(Map())
      extraction.months.foreach {
        case "January" =>
          boostLocation(clim)("jan")(location)
        case "February" =>
          boostLocation(clim)("feb")(location)
        case "March" =>
          boostLocation(clim)("mar")(location)
        case "April" =>
          boostLocation(clim)("apr")(location)
        case "May" =>
          boostLocation(clim)("may")(location)
        case "June" =>
          boostLocation(clim)("jun")(location)
        case "July" =>
          boostLocation(clim)("jul")(location)
        case "August" =>
          boostLocation(clim)("aug")(location)
        case "September" =>
          boostLocation(clim)("sep")(location)
        case "October" =>
          boostLocation(clim)("oct")(location)
        case "November" =>
          boostLocation(clim)("nov")(location)
        case "December" =>
          boostLocation(clim)("dec")(location)
      }
    }
    locations
  }

  /**
   * Parses query and extracts mentioned countries, languages and temperature.
   * Extraction is based on  language pattern.
   * @param text Query string
   * @return Extracted information
   */
  def parseQuery(text: String) = {

    //finds all matches within given string and certain regex
    val search: Seq[Regex] => String => Seq[String] = rs => s => rs.flatMap(r => for(c <- r.findAllMatchIn(s)) yield c.group(2))


    //find all mentioned countries
    val countries =  search(SpeechPattern.countries)(text)
    //find all mentioned languages
    val languages = search(SpeechPattern.language)(text)

    implicit def StringToFloat(s: String): Option[Float] = Try(Some(s.trim.toFloat)).getOrElse(None)

    //extracts temperature value from string
    val cleanTemperature: String => String = s => {
      val Number = "\\W?[0-9]+\\.?[0-9]*".r
      Number.findFirstIn(s).getOrElse("").replaceAll("[(|)]","")
    }

    type Range =  (Float, Float)
    implicit def seqToRange(a: Array[Float]): Option[Range] = if(a.length == 2) Some(a.head,a.tail.head) else None

    //extracts a range in a single string
    val extractRnge: String => Option[Range] = range => {
      val improvedRange = range.replaceAll("\\p{Pd}", 45.toChar.toString)
      val split = if(improvedRange.contains("to")) improvedRange.split("to") else improvedRange.split(45.toChar.toString)
      split.map(cleanTemperature).filter(_.isDefined).map(_.get)
    }

    //extract all mentioned temperature ranges within text
    val range: Seq[Regex] => String => Seq[Range] = r => q => search(r)(q).view.map(extractRnge)
      .filter(range => range.isDefined).map(range => range.get)

    //temperature ranges which were mentioned in the query
    val temperatureRange = {
      val c: Seq[Range] => Seq[Range] = ranges => ranges.map(r =>  (conv(r._1),conv(r._2)))
      val rangeC = range(SpeechPattern.rangeC)(text)
      rangeC.size match {
        case 0 => c(range(SpeechPattern.rangeF)(text))
        case _ => rangeC
      }
    }

    //all temperature mentions within the query text
    val temperature: () => Seq[Float] = () => {
      val temperatureC = search(SpeechPattern.temperatureC)(text).view.map(cleanTemperature).filter(_.isDefined).map(_.get)
      temperatureC.size match {
        case 0 =>  search(SpeechPattern.temperatureF)(text).view.map(cleanTemperature).filter(_.isDefined).map(v => conv(v.get))
        case _ => temperatureC
      }
    }

    val months = search(SpeechPattern.month)(text)

    new Extraction(languages, countries, temperatureRange, if(temperatureRange.size==0) temperature() else Seq(), months)
  }

  case class Extraction(languages: Seq[String], countries: Seq[String],range: Seq[(Float,Float)], temperature: Seq[Float], months: Seq[String])
}

object SpeechPattern {

  //regex for all possible languages
  lazy val popularLanguages = "(.*?)\\W(Mandarin|Spanish|Spanish|Hindi|Arabic|Portuguese|Bengali|Russian|Japanese|Punjabi|German|Javanese|Wu|Malay|Indonesian|Telugu|Vietnamese|Korean|French|Marathi|Tamil|Urdu|Persian|Turkish|Italian|Cantonese|Thai|Gujarati|Jin|Min Nan|Polish|Pashto|Kannada|Xiang|Malayalam|Sundanese|Hausa|Oriya|Burmese|Hakka|Ukrainian|Bhojpuri|Tagalog|Yoruba|Maithili|Swahili|Uzbek|Sindhi|Amharic|Fula|Romanian|Oromo|Igbo|Azerbaijani|Awadhi|Gan|Cebuano|Dutch|Kurdish|Serbo-Croatian|Malagasy|Saraiki|Nepali|Sinhalese|Chittagonian|Zhuang|Khmer|Assamese|Madurese|Somali|Marwari|Magahi|Haryanvi|Hungarian|Chhattisgarhi|Greek|Chewa|Deccan|Akan|Kazakh|Min\\sBei|Sylheti|Zulu|Czech|Kinyarwanda|Dhundhari|Haitian\\sCreole|Min\\sDong|Ilokano|Quechua|Kirundi|Swedish|Hmong|Shona|Uyghur|Hiligaynon|Mossi|Xhosa|Belarusian|Balochi|Konkani)\\W(.*?)".r
  lazy val language = Seq(popularLanguages)

  //regex for all countries
  lazy val countries = Seq("(.*?)\\W(Afghanistan|Albania|Algeria|Andorra|Angola|Antigua\\sand\\sDeps|Argentina|Armenia|Australia|Austria|Azerbaijan|Bahamas|Bahrain|Bangladesh|Barbados|Belarus|Belgium|Belize|Benin|Bhutan|Bolivia|Bosnia Herzegovina|Botswana|Brazil|Brunei|Bulgaria|Burkina|Burundi|Cambodia|Cameroon|Canada|Cape Verde|Central African Rep|Chad|Chile|China|Colombia|Comoros|Congo|Congo Democratic Rep|Costa Rica|Croatia|Cuba|Cyprus|Czech Republic|Denmark|Djibouti|Dominica|Dominican Republic|East Timor|Ecuador|Egypt|El Salvador|Equatorial Guinea|Eritrea|Estonia|Ethiopia|Fiji|Finland|France|Gabon|Gambia|Georgia|Germany|Ghana|Greece|Grenada|Guatemala|Guinea|Guinea-Bissau|Guyana|Haiti|Honduras|Hungary|Iceland|India|Indonesia|Iran|Iraq|Ireland|Ireland Republic|Israel|Italy|Ivory Coast|Jamaica|Japan|Jordan|Kazakhstan|Kenya|Kiribati|Korea North|Korea South|Kosovo|Kuwait|Kyrgyzstan|Laos|Latvia|Lebanon|Lesotho|Liberia|Libya|Liechtenstein|Lithuania|Luxembourg|Macedonia|Madagascar|Malawi|Malaysia|Maldives|Mali|Malta|Marshall Islands|Mauritania|Mauritius|Mexico|Micronesia|Moldova|Monaco|Mongolia|Montenegro|Morocco|Mozambique|Myanmar, Burma|Namibia|Nauru|Nepal|Netherlands|New Zealand|Nicaragua|Niger|Nigeria|Norway|Oman|Pakistan|Palau|Panama|Papua New Guinea|Paraguay|Peru|Philippines|Poland|Portugal|Qatar|Romania|Russian Federation|Russia|Rwanda|St Kitts and Nevis|St Lucia|Saint Vincent & the Grenadines|Samoa|San Marino|Sao Tome and Principe|Saudi Arabia|Senegal|Serbia|Seychelles|Sierra Leone|Singapore|Slovakia|Slovenia|Solomon Islands|Somalia|South Africa|South Sudan|Spain|Sri Lanka|Sudan|Suriname|Swaziland|Sweden|Switzerland|Syria|Taiwan|Tajikistan|Tanzania|Thailand|Togo|Tonga|Trinidad and Tobago|Tunisia|Turkey|Turkmenistan|Tuvalu|Uganda|Ukraine|United Arab Emirates|United Kingdom|UK|England|United States|US|USA|usa|U\\.S\\.A\\.|u\\.s\\.a\\.|America|Uruguay|Uzbekistan|Vanuatu|Vatican City|Venezuela|Vietnam|Yemen|Zambia|Zimbabwe)\\W(.*?)".r)

  //regex for temperature
  lazy val temperatureC = Seq("(.*?)\\W(\\d+\\sC|\\d+\\s°C|\\d+\\.\\d+\\sC|\\d+\\.\\d+\\s°C|\\W\\d+\\sC|\\W\\d+\\s°C|\\W\\d+\\.\\d+\\sC|\\W\\d+\\.\\d+\\s°C)\\W(.*?)".r)
  lazy val rangeC = Seq("(.*?)\\W(\\d+\\W\\d+\\sC|\\d+\\W\\d+\\s°C|\\W\\d+\\W\\d+\\sC|\\W\\d+\\W\\d+\\s°C|\\d+\\sto\\s\\d+\\sC|\\d+\\sto\\s\\d+\\s°C|\\W\\d+\\sto\\s\\d+\\sC|\\W\\d+\\sto\\s\\d+\\s°C|\\W\\d+\\sto\\s\\W\\d+\\sC|\\W\\d+\\sto\\s\\W\\d+\\s°C)\\W(.*?)".r)
  lazy val temperatureF = Seq("(.*?)\\W(\\d+\\sF|\\d+\\s°F|\\d+\\.\\d+\\sF|\\d+\\.\\d+\\s°F|\\W\\d+\\sF|\\W\\d+\\s°F|\\W\\d+\\.\\d+\\sF|\\W\\d+\\.\\d+\\s°F)\\W(.*?)".r)
  lazy val rangeF = Seq("(.*?)\\W(\\d+\\W\\d+\\sF|\\d+\\W\\d+\\s°F|\\W\\d+\\W\\d+\\sF|\\W\\d+\\W\\d+\\s°F|\\d+\\sto\\s\\d+\\sF|\\d+\\sto\\s\\d+\\s°F|\\W\\d+\\sto\\s\\d+\\sF|\\W\\d+\\sto\\s\\d+\\s°F|\\W\\d+\\sto\\s\\W\\d+\\sF|\\W\\d+\\sto\\s\\W\\d+\\s°F)\\W(.*?)".r)

  lazy val month = Seq("(.*?)\\W(January|February|March|April|May|June|July|August|September|October|November|December)\\W(.*?)".r)

}
