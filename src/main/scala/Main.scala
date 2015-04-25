/**
 * Created by yevgen on 11.04.15.
 */

import clavin.ClavinClient
import dbpedia.{YagoGeoTypes, SpotlightClient, DBPediaLookup}
import edu.knowitall.openie.{TemporalArgument, SpatialArgument, SimpleArgument, OpenIE}

import elasticsearch.ElasticsearchClient
import nlp.{RelationExtractor, StanfordAnnotator}
import core.SparqlQueryCreator
import tools.Levenshtein

object Main  extends App{

  val s =
    s"""
       |New York City (standardsprachlich (AE) [nuːˈjɔɹk ˈsɪɾi]; BE: [ˈnjuːˈjɔːk ˈsɪti]), kurz: New York (veraltet: Neuyork[1]), Abk.: NYC, ist eine Weltstadt an der Ostküste der Vereinigten Staaten. Sie liegt im Bundesstaat New York und ist mit mehr als acht Millionen Einwohnern die bevölkerungsreichste Stadt der Vereinigten Staaten.[2] Die Stadtverwaltung trägt den Namen City of New York.
       |
       |Die Metropolregion New York mit 18,9 Millionen Einwohnern[3] ist einer der bedeutendsten Wirtschaftsräume und Handelsplätze der Welt, Sitz vieler internationaler Konzerne und Organisationen wie der Vereinten Nationen sowie wichtiger Seehafen an der amerikanischen Ostküste. Die Stadt genießt mit ihrer großen Anzahl an Sehenswürdigkeiten, den 500 Galerien, etwa 200 Museen, mehr als 150 Theatern und mehr als 18.000 Restaurants Weltruf auch in den Bereichen Kunst und Kultur und verzeichnet jedes Jahr etwa 50 Millionen Besucher.[4] Laut Forbes Magazine ist New York City die Stadt mit den höchsten Lebenshaltungskosten in den Vereinigten Staaten sowie eine der teuersten Städte weltweit.[5]
       |
       |Nachdem 1524 Giovanni da Verrazano und 1609 Henry Hudson die Gegend des heutigen New Yorks erforscht hatten, siedelten ab 1610 niederländische Kaufleute an der Südspitze der Insel Manna-Hatta und bald darauf an der Westspitze von Long Island, dem heutigen Brooklyn. Erst 1626 kaufte Peter Minuit den Einheimischen, wahrscheinlich Lenni-Lenape-Indianern, die Insel „Manna-hatta“ für Waren im Wert von 60 Gulden ab. Die damit begründete Siedlung erhielt danach den Namen Nieuw Amsterdam und war zunächst Hauptstadt der Kolonie Nieuw Nederland, bis sie 1664 von den Briten erobert wurde und die Stadt den seither gültigen Namen bekam.[6] Ihr Aufstieg zur Weltstadt begann 1825 mit der Fertigstellung des Eriekanals.
     """.stripMargin

  /*
  val stanford = new StanfordAnnotator
  stanford.annotateText(s)
  //stanford.annotateText(s + "bla")
  */


  val query = new SparqlQueryCreator
  query.createSparqlQuery(s)

  /*
  val clavin = new ClavinClient()
  val l = "Paris is a nice cite."
  println(clavin.extractLocations(s))


  val openie = new RelationExtractor
  println(openie.extractRelations(s))


  val spotlightClient = new SpotlightClient
  println(spotlightClient.discoverEntities(s))

  */

/*
  val openie = new RelationExtractor
  openie.extractRelations(s).foreach{
    c => println(c)
  }
*/
/*
  val elastic = new ElasticsearchClient
  elastic.findDBPediaClasses("Place").foreach{
    x => println(x)
  }
*/
/*
  val openie = new RelationExtractor
  openie.extractRelations(s).foreach{
    x => println(x)
  }
*/

}
