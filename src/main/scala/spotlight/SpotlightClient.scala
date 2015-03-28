package spotlight

import scalaj.http._


//This class represents spotlight client, using remote dbpedia endpoint
class RemoteSpotlightClient {

  //return: Result as json string
  def requestLocation(text: String): String = {
    val response: HttpResponse[String] = Http("http://spotlight.sztaki.hu:2222/rest/annotate").param("text", text)
      .param("confidence", "0.25").param("support","5").param("types","Place").header("Accept","application/json").asString
    response.body
  }

}



