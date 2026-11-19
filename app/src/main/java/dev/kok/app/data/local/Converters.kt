package dev.kok.app.data.local

import androidx.room.TypeConverter
import dev.kok.app.domain.model.ActivityLevel
import dev.kok.app.domain.model.BodyRegion
import dev.kok.app.domain.model.ChatRole
import dev.kok.app.domain.model.EntrySource
import dev.kok.app.domain.model.FoodCategory
import dev.kok.app.domain.model.MuscleRole
import dev.kok.app.domain.model.Sex
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** Tarihler ISO metin olarak saklanır: sözlük sırası kronolojik sıraya eşittir. */
class Converters {
    @TypeConverter fun localDateToString(v: LocalDate?): String? = v?.toString()
    @TypeConverter fun stringToLocalDate(v: String?): LocalDate? = v?.let(LocalDate::parse)

    @TypeConverter fun yearMonthToString(v: YearMonth?): String? = v?.toString()
    @TypeConverter fun stringToYearMonth(v: String?): YearMonth? = v?.let(YearMonth::parse)

    @TypeConverter fun instantToLong(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)

    @TypeConverter fun sexToString(v: Sex): String = v.name
    @TypeConverter fun stringToSex(v: String): Sex = Sex.valueOf(v)

    @TypeConverter fun activityToString(v: ActivityLevel): String = v.name
    @TypeConverter fun stringToActivity(v: String): ActivityLevel = ActivityLevel.valueOf(v)

    @TypeConverter fun sourceToString(v: EntrySource): String = v.name
    @TypeConverter fun stringToSource(v: String): EntrySource = EntrySource.valueOf(v)

    @TypeConverter fun categoryToString(v: FoodCategory): String = v.name
    @TypeConverter fun stringToCategory(v: String): FoodCategory = FoodCategory.valueOf(v)

    @TypeConverter fun roleToString(v: MuscleRole): String = v.name
    @TypeConverter fun stringToRole(v: String): MuscleRole = MuscleRole.valueOf(v)

    @TypeConverter fun chatRoleToString(v: ChatRole): String = v.name
    @TypeConverter fun stringToChatRole(v: String): ChatRole = ChatRole.valueOf(v)

    @TypeConverter fun regionToString(v: BodyRegion): String = v.name
    @TypeConverter fun stringToRegion(v: String): BodyRegion = BodyRegion.valueOf(v)
}
