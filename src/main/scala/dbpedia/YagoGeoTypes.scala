package dbpedia

/**
 * Created by yevgen on 13.04.15.
 */
object YagoGeoTypes {

  val prefix = "http://dbpedia.org/class/yago/"
  val geoEntity = prefix + "YagoGeoEntity"

  val types = Map("city" -> "City108524735","country" -> "Country108544813","dam" -> "Dam103160309","desert" -> "Desert108505573",
  "garden" -> "Garden103417345", "museum" -> "Museum103800563", "mountain" -> "Mountain109359803", "hill" -> "Hill109303008",
  "island" -> "Island109316454", "lake" -> "Lake109328904", "building" -> "Building102913152", "house" -> "House103544360",
  "housing" -> "Housing103546340", "monastery" -> "Monastery103781244", "mosque" -> "Mosque103788195", "area" -> "Area108497294",
  "oasis" -> "Oasis108506496", "oilfield" -> "Oilfield108659861", "field" -> "Field108659446", "park" -> "Park108615149",
"river"->"River109411430", "temple" -> "Temple104407435", "skyscraper" -> "Skyscraper104233124",
    "restaurant" -> "Restaurant104081281", "town" -> "Town108665504", "village" -> "Village108672738", "fishing" -> "FishingVillages")

  val keys = types.keySet

  def getYagoEntities(text: String): List[String] = {
    val lower = text.toLowerCase
    types.filterKeys(x => lower.contains(x)).map(k => prefix + k._2).toList
  }
}
