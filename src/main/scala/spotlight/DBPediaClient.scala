package spotlight

import scalaj.http.{Http, HttpResponse}

/**
 * Created by yevgen on 28.03.15.
 */
class DBPediaClient {

  def request(uri: String): String = {
    val response: HttpResponse[String] = Http(uri).header("Accept","application/json").asString
    response.body
  }
}
