package com.example.data

data class SoberingVerse(
    val reference: String,
    val text: String,
    val soberingThought: String
)

object SoberingVersesProvider {

    val VERSES = listOf(
        SoberingVerse(
            reference = "1ኛ ቆሮንቶስ 6:18",
            text = "«ከዝሙት ሽሹ! ሰው የሚያደርገው ኃጢአት ሁሉ ከሥጋ ውጭ ነው፤ ዝሙትን የሚሠራ ግን በገዛ ሥጋው ላይ ኃጢአት ይሠራል!»",
            soberingThought = "ለአምስት ደቂቃ ጊዜያዊና ርካሽ ስሜት ብለህ ቅዱሱን የፈጣሪህን ማደሪያ ሥጋህን አታርክስ! አሁኑኑ ተመለስ!"
        ),
        SoberingVerse(
            reference = "ምሳሌ 4:23",
            text = "«አእምሮህን ከሁሉ በላይ ጠብቅ፤ የሕይወት ምንጭ ከእርሱ ይወጣልና።»",
            soberingThought = "ዓይንህ የሚያየው ነገር ወደ ልብህ ይገባል፤ ልብህ የረከሰ ዕለት ሕይወትህ ይጨልማል። አእምሮህን በንጽህና ጠብቅ!"
        ),
        SoberingVerse(
            reference = "ምሳሌ 6:27-28",
            text = "«እሳት በብብቱ ቋጥሮ ልብሱ የማይቃጠል ማን ነው? በፍም ላይ ሄዶ እግሩ የማይቃጠል ማን ነው?»",
            soberingThought = "ከእሳት ጋር አትጫወት! ይህ የጀመርከው መንገድ መጨረሻው ጸጸት፣ ውርደት፣ ጭንቀትና የህሊና ባዶነት ብቻ ነው። ቆም ብለህ አስብ!"
        ),
        SoberingVerse(
            reference = "ማቴዎስ 16:26",
            text = "«ሰው ዓለምን ሁሉ ቢያተርፍ ነፍሱንም ቢያጎድል ምን ይጠቅመዋል? ወይስ ሰው ስለ ነፍሱ ቤዛ ምን ይሰጣል?»",
            soberingThought = "እግዚአብሔር አሁን እያየህ ነው! ይህ የስሜት ማዕበል ውሸት ነው፤ የከበረችውን ነፍስህን ለዚህ እርካሽ ነገር አትሸጣት!"
        ),
        SoberingVerse(
            reference = "ማቴዎስ 5:28",
            text = "«እኔ ግን እላችኋለሁ፥ ወደ ሴት ያየ ሁሉ የተመኛትም ያን ጊዜ በልቡ ከእርስዋ ጋር አመንዝሮአል።»",
            soberingThought = "የተፈጠርከው ለታላቅ ክብርና ለተቀደሰ ዓላማ ነው እንጂ ለስልክ ስክሪንና ለስሜትህ ባሪያ እንድትሆን አይደለም!"
        ),
        SoberingVerse(
            reference = "ሮሜ 8:6",
            text = "«ስለ ሥጋ ማሰብ ሞት ነው፥ ስለ መንፈስ ማሰብ ግን ሕይወትና ሰላም ነው።»",
            soberingThought = "ይህ ስሜት ልክ እንደተወጣኸው የሚያመጣው ሰላም የለም፤ ጥቁር ጸጸትና እራስን መጥላት ብቻ ነው። ጥንካሬህን አሳየውና ዝጋው!"
        ),
        SoberingVerse(
            reference = "1ኛ ጴጥሮስ 2:11",
            text = "«ወዳጆች ሆይ፥ ነፍስን ከሚዋጋ ሥጋዊ ምኞት ትርቁ ዘንድ እንደ እንግዶችና እንደ መጻተኞች ሆናችሁ እለምናችኋለሁ።»",
            soberingThought = "ይህ ምኞት ጠላትህ ነው፤ ነፍስህን ሊያጠፋ እየተዋጋህ ነው። እጅህን አንሳ፣ ስልክህን አስቀምጠህ ጥልቅ አየር ተንፍስ!"
        )
    )

    fun getRandomVerse(): SoberingVerse {
        return VERSES.random()
    }
}
