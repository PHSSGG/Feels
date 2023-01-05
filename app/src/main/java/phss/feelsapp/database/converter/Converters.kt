package phss.feelsapp.database.converter

import androidx.room.TypeConverter
import java.util.*

object Converters {

    @TypeConverter
    fun stringListToString(list: List<String>): String {
        var string = "["

        for (selected in list) {
            if (list.getOrNull(list.indexOf(selected) + 1) == null) {
                string += if (string == "") selected
                else ", $selected"
                break
            }

            string += if (string == "[") selected else ", $selected"
        }

        string += "]"
        return string
    }

    @TypeConverter
    fun stringToStringList(string: String): List<String> {
        val list = ArrayList<String>()
        for (selected in string.replace("[", "").replace("]", "").split(",")) {
            list.add(selected.replace(" ", ""))
        }

        return list
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

}