package com.example.orbitfocus;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Dil yöneticisi - Çoklu dil desteği.
 * TR, EN, RU, ZH, DE, FR
 */
public class LanguageManager {

    private static final String PREFS_NAME = "PlantagePrefs";
    private static final String KEY_LANGUAGE = "language";

    public static final String LANG_TR = "tr"; // Türkçe
    public static final String LANG_EN = "en"; // English
    public static final String LANG_RU = "ru"; // Русский
    public static final String LANG_ZH = "zh"; // 中文
    public static final String LANG_DE = "de"; // Deutsch
    public static final String LANG_FR = "fr"; // Français

    private Context context;
    private SharedPreferences prefs;

    public LanguageManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, LANG_EN); // Default: English
    }

    public void setLanguage(String lang) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply();
    }

    public boolean isTurkish() {
        return LANG_TR.equals(getLanguage());
    }

    public boolean isEnglish() {
        return LANG_EN.equals(getLanguage());
    }

    public boolean isRussian() {
        return LANG_RU.equals(getLanguage());
    }

    public boolean isChinese() {
        return LANG_ZH.equals(getLanguage());
    }

    public boolean isGerman() {
        return LANG_DE.equals(getLanguage());
    }

    public boolean isFrench() {
        return LANG_FR.equals(getLanguage());
    }

    // ==================== QUOTES ====================

    private static final String[] QUOTES_TR = {
            "Anılarımız, biz onları besledikçe büyüyen bir ağaçtır.",
            "Bugün toprağa bıraktığın bir anı, yarın gölgesinde dinleneceğin bir ağaç olur.",
            "Köklerimiz ne kadar derine inerse, dallarımız o kadar göğe uzanır.",
            "Zaman akıp gider, ama anılar olduğu yere kök salar.",
            "Geçmiş, bugünün toprağıdır; ne ekersen geleceğinde o yeşerir.",
            "Yaşanan her gün, hayat ağacının gövdesine eklenen sağlam bir halkadır.",
            "En ulu ormanlar bile sessiz bir tohumla başlar.",
            "Ruhunu yeşerten her hatıra, asla solmayan bir yapraktır.",
            "Sabırla büyüyen her dal, zamanın bize en güzel hediyesidir.",
            "Bugün açan bir yaprak, yarının hikayesidir.",
            "Hayat aceleye gelmez; tıpkı bir ağaç gibi, günbegün büyür.",
            "Kağıda dökülen her söz, sonsuzluğa atılan bir tohumdur.",
            "Senin hikayen, bu ağacın can suyudur.",
            "Unutulup gitmesine izin verme; her anı bir yaprak olmayı hak eder.",
            "Sessizce biriken anılar, en gürültülü zamanlarda sığınağımızdır.",
            "Hiçbir yaprak diğerine benzemez, tıpkı senin eşsiz anıların gibi."
    };

    private static final String[] QUOTES_EN = {
            "Our memories are a tree that grows as we nurture them.",
            "A memory planted today becomes a tree you'll rest under tomorrow.",
            "The deeper our roots go, the higher our branches reach toward the sky.",
            "Time flows on, but memories take root where they stand.",
            "The past is the soil of today; what you plant will bloom in your future.",
            "Each day lived is a strong ring added to the trunk of life's tree.",
            "Even the mightiest forests begin with a silent seed.",
            "Every memory that nourishes your soul is a leaf that never fades.",
            "Every branch that grows with patience is time's greatest gift to us.",
            "A leaf that opens today is tomorrow's story.",
            "Life cannot be rushed; like a tree, it grows day by day.",
            "Every word put to paper is a seed thrown into eternity.",
            "Your story is the lifeblood of this tree.",
            "Don't let it fade away; every moment deserves to be a leaf.",
            "Quietly gathered memories are our refuge in the noisiest times.",
            "No leaf is like another, just like your unique memories."
    };

    private static final String[] QUOTES_RU = {
            "Наши воспоминания — это дерево, которое растёт, когда мы его питаем.",
            "Воспоминание, посаженное сегодня, станет деревом, под которым ты отдохнёшь завтра.",
            "Чем глубже наши корни, тем выше наши ветви тянутся к небу.",
            "Время течёт, но воспоминания пускают корни там, где они есть.",
            "Прошлое — это почва сегодняшнего дня; что посеешь, то и взойдёт в будущем.",
            "Каждый прожитый день — это прочное кольцо на стволе древа жизни.",
            "Даже самые могучие леса начинаются с тихого семени.",
            "Каждое воспоминание, питающее душу — это лист, который никогда не увянет.",
            "Каждая ветвь, растущая с терпением — величайший дар времени.",
            "Лист, раскрывшийся сегодня — это завтрашняя история.",
            "Жизнь не терпит спешки; как дерево, она растёт день за днём.",
            "Каждое слово на бумаге — это семя, брошенное в вечность.",
            "Твоя история — это живительная сила этого дерева.",
            "Не дай ей исчезнуть; каждый момент заслуживает стать листом.",
            "Тихо собранные воспоминания — наше убежище в самые шумные времена.",
            "Ни один лист не похож на другой, как и твои уникальные воспоминания."
    };

    private static final String[] QUOTES_ZH = {
            "我们的回忆是一棵树，只要我们用心培育，它就会成长。",
            "今天种下的回忆，明天将成为你休憩的大树。",
            "根扎得越深，枝条就能伸向更高的天空。",
            "时间流逝，但回忆会在原地生根。",
            "过去是今天的土壤；你播种什么，未来就会收获什么。",
            "每过一天，生命之树的树干上就多了一个坚实的年轮。",
            "即使是最茂盛的森林，也始于一颗静默的种子。",
            "每一个滋养心灵的回忆，都是永不凋零的叶子。",
            "每一根耐心生长的枝条，都是时间给我们最好的礼物。",
            "今天展开的叶子，就是明天的故事。",
            "生命不能急躁；就像树一样，日复一日地成长。",
            "写下的每一个字，都是撒向永恒的种子。",
            "你的故事，是这棵树的生命之水。",
            "不要让它消逝；每一刻都值得成为一片叶子。",
            "静静积累的回忆，是我们在喧嚣中的避风港。",
            "没有两片叶子是相同的，就像你独特的回忆。"
    };

    private static final String[] QUOTES_DE = {
            "Unsere Erinnerungen sind ein Baum, der wächst, wenn wir ihn pflegen.",
            "Eine heute gepflanzte Erinnerung wird morgen ein Baum, unter dem du ruhen kannst.",
            "Je tiefer unsere Wurzeln reichen, desto höher streben unsere Äste zum Himmel.",
            "Die Zeit vergeht, aber Erinnerungen schlagen dort Wurzeln, wo sie stehen.",
            "Die Vergangenheit ist der Boden von heute; was du säst, wird in deiner Zukunft blühen.",
            "Jeder gelebte Tag ist ein starker Ring am Stamm des Lebensbaums.",
            "Selbst die mächtigsten Wälder beginnen mit einem stillen Samen.",
            "Jede Erinnerung, die die Seele nährt, ist ein Blatt, das nie verwelkt.",
            "Jeder Ast, der mit Geduld wächst, ist das größte Geschenk der Zeit.",
            "Ein Blatt, das sich heute öffnet, ist die Geschichte von morgen.",
            "Das Leben lässt sich nicht hetzen; wie ein Baum wächst es Tag für Tag.",
            "Jedes zu Papier gebrachte Wort ist ein Samen, der in die Ewigkeit geworfen wird.",
            "Deine Geschichte ist das Lebenselixier dieses Baumes.",
            "Lass es nicht verblassen; jeder Moment verdient es, ein Blatt zu sein.",
            "Still gesammelte Erinnerungen sind unsere Zuflucht in den lautesten Zeiten.",
            "Kein Blatt gleicht dem anderen, genau wie deine einzigartigen Erinnerungen."
    };

    private static final String[] QUOTES_FR = {
            "Nos souvenirs sont un arbre qui grandit à mesure que nous les nourrissons.",
            "Un souvenir planté aujourd'hui deviendra l'arbre sous lequel tu te reposeras demain.",
            "Plus nos racines s'enfoncent, plus nos branches s'élèvent vers le ciel.",
            "Le temps passe, mais les souvenirs s'enracinent là où ils se trouvent.",
            "Le passé est le terreau d'aujourd'hui ; ce que tu sèmes fleurira dans ton avenir.",
            "Chaque jour vécu est un anneau solide ajouté au tronc de l'arbre de vie.",
            "Même les forêts les plus majestueuses commencent par une graine silencieuse.",
            "Chaque souvenir qui nourrit l'âme est une feuille qui ne fane jamais.",
            "Chaque branche qui grandit avec patience est le plus beau cadeau du temps.",
            "Une feuille qui s'ouvre aujourd'hui est l'histoire de demain.",
            "La vie ne peut être pressée ; comme un arbre, elle grandit jour après jour.",
            "Chaque mot couché sur papier est une graine jetée dans l'éternité.",
            "Ton histoire est la sève de cet arbre.",
            "Ne la laisse pas s'effacer ; chaque moment mérite d'être une feuille.",
            "Les souvenirs silencieusement rassemblés sont notre refuge dans les moments les plus bruyants.",
            "Aucune feuille ne ressemble à une autre, tout comme tes souvenirs uniques."
    };

    public String[] getQuotes() {
        String lang = getLanguage();
        switch (lang) {
            case LANG_EN:
                return QUOTES_EN;
            case LANG_RU:
                return QUOTES_RU;
            case LANG_ZH:
                return QUOTES_ZH;
            case LANG_DE:
                return QUOTES_DE;
            case LANG_FR:
                return QUOTES_FR;
            default:
                return QUOTES_TR;
        }
    }

    // ==================== UI STRINGS ====================

    private String getString(String tr, String en, String ru, String zh, String de, String fr) {
        String lang = getLanguage();
        switch (lang) {
            case LANG_EN:
                return en;
            case LANG_RU:
                return ru;
            case LANG_ZH:
                return zh;
            case LANG_DE:
                return de;
            case LANG_FR:
                return fr;
            default:
                return tr;
        }
    }

    // Settings
    public String getSettings() {
        return getString("⚙️ Ayarlar", "⚙️ Settings", "⚙️ Настройки", "⚙️ 设置", "⚙️ Einstellungen", "⚙️ Paramètres");
    }

    public String getMusicVolume() {
        return getString("🔊 Müzik Sesi", "🔊 Music Volume", "🔊 Громкость", "🔊 音乐音量", "🔊 Musiklautstärke",
                "🔊 Volume");
    }

    public String getLanguage_() {
        return getString("🌐 Dil", "🌐 Language", "🌐 Язык", "🌐 语言", "🌐 Sprache", "🌐 Langue");
    }

    public String getOk() {
        return getString("Tamam", "OK", "ОК", "确定", "OK", "OK");
    }

    public String getCancel() {
        return getString("İptal", "Cancel", "Отмена", "取消", "Abbrechen", "Annuler");
    }

    public String getSave() {
        return getString("Kaydet", "Save", "Сохранить", "保存", "Speichern", "Enregistrer");
    }

    public String getClose() {
        return getString("Kapat", "Close", "Закрыть", "关闭", "Schließen", "Fermer");
    }

    public String getDelete() {
        return getString("🗑️ Sil", "🗑️ Delete", "🗑️ Удалить", "🗑️ 删除", "🗑️ Löschen", "🗑️ Supprimer");
    }

    // Leaf dialog
    public String getNextLeaf() {
        return getString("Sonraki Yaprak", "Next Leaf", "Следующий лист", "下一片叶子", "Nächstes Blatt",
                "Prochaine feuille");
    }

    public String getPhotos() {
        return getString("📷 Fotoğraflar", "📷 Photos", "📷 Фотографии", "📷 照片", "📷 Fotos", "📷 Photos");
    }

    public String getAddPhoto() {
        return getString("+ Fotoğraf Ekle", "+ Add Photo", "+ Добавить фото", "+ 添加照片", "+ Foto hinzufügen",
                "+ Ajouter photo");
    }

    public String getWriteMemory() {
        return getString("📝 Anını Yaz", "📝 Write Your Memory", "📝 Напиши воспоминание", "📝 写下回忆",
                "📝 Schreibe deine Erinnerung", "📝 Écris ton souvenir");
    }

    public String getWhatHappenedToday() {
        return getString("Bugün ne oldu?", "What happened today?", "Что произошло сегодня?", "今天发生了什么？",
                "Was ist heute passiert?", "Que s'est-il passé aujourd'hui ?");
    }

    public String getMemorySaved() {
        return getString("Anın kaydedildi! 🌿", "Memory saved! 🌿", "Воспоминание сохранено! 🌿", "回忆已保存！🌿",
                "Erinnerung gespeichert! 🌿", "Souvenir enregistré ! 🌿");
    }

    public String getPhotoAdded() {
        return getString("Fotoğraf eklendi!", "Photo added!", "Фото добавлено!", "照片已添加！", "Foto hinzugefügt!",
                "Photo ajoutée !");
    }

    public String getPhotoDeleted() {
        return getString("Fotoğraf silindi", "Photo deleted", "Фото удалено", "照片已删除", "Foto gelöscht",
                "Photo supprimée");
    }

    public String getLeafWithered() {
        return getString("Yaprak kurudu 🍂", "Leaf withered 🍂", "Лист увял 🍂", "叶子枯萎了 🍂", "Blatt verwelkt 🍂",
                "Feuille fanée 🍂");
    }

    // Leaf status titles
    public String getActiveLeafTitle() {
        return getString("🌱 Bugünün Yaprağı", "🌱 Today's Leaf", "🌱 Лист сегодня", "🌱 今天的叶子", "🌱 Heutiges Blatt",
                "🌱 Feuille du jour");
    }

    public String getLockedLeafTitle() {
        return getString("🔒 Kilitli Anı", "🔒 Locked Memory", "🔒 Заблокировано", "🔒 锁定的回忆",
                "🔒 Gesperrte Erinnerung", "🔒 Souvenir verrouillé");
    }

    public String getWitheredLeafTitle() {
        return getString("🍂 Kurumuş Yaprak", "🍂 Withered Leaf", "🍂 Увядший лист", "🍂 枯萎的叶子", "🍂 Verwelktes Blatt",
                "🍂 Feuille fanée");
    }

    // Locked leaf
    public String getLockedMemoryInfo() {
        return getString(
                "🔒 Bu anı kilitlendi ve sonsuza kadar korunuyor.",
                "🔒 This memory is locked and preserved forever.",
                "🔒 Это воспоминание заблокировано навсегда.",
                "🔒 这段回忆已被锁定并永久保存。",
                "🔒 Diese Erinnerung ist gesperrt und für immer bewahrt.",
                "🔒 Ce souvenir est verrouillé et préservé pour toujours.");
    }

    public String getNote() {
        return getString("📝 Not", "📝 Note", "📝 Заметка", "📝 笔记", "📝 Notiz", "📝 Note");
    }

    // Withered leaf
    public String getMissedThisDay() {
        return getString("Bu günü kaçırdın...", "You missed this day...", "Ты пропустил этот день...", "你错过了这一天...",
                "Du hast diesen Tag verpasst...", "Tu as manqué ce jour...");
    }

    public String getNoMemoryAdded() {
        return getString(
                "Bu güne ait bir anı eklenmedi ve yaprak kurudu.",
                "No memory was added for this day and the leaf withered.",
                "Воспоминание не было добавлено, и лист увял.",
                "这一天没有添加回忆，叶子枯萎了。",
                "Keine Erinnerung wurde hinzugefügt und das Blatt ist verwelkt.",
                "Aucun souvenir n'a été ajouté et la feuille a fané.");
    }

    public String getUnderstood() {
        return getString("Anladım", "I Understand", "Понятно", "我明白了", "Verstanden", "J'ai compris");
    }

    // Delete confirmation
    public String getDeleteLeafTitle() {
        return getString("🗑️ Yaprağı Sil", "🗑️ Delete Leaf", "🗑️ Удалить лист", "🗑️ 删除叶子", "🗑️ Blatt löschen",
                "🗑️ Supprimer feuille");
    }

    public String getDeleteConfirmation() {
        return getString(
                "Bu yaprağı silmek istediğinize emin misiniz?",
                "Are you sure you want to delete this leaf?",
                "Вы уверены, что хотите удалить этот лист?",
                "你确定要删除这片叶子吗？",
                "Bist du sicher, dass du dieses Blatt löschen möchtest?",
                "Es-tu sûr de vouloir supprimer cette feuille ?");
    }

    public String getYesDelete() {
        return getString("Evet, Sil", "Yes, Delete", "Да, удалить", "是的，删除", "Ja, löschen", "Oui, supprimer");
    }

    // App info
    public String getAppName() {
        return "Plantage";
    }

    // Language changed
    public String getLanguageChanged() {
        return getString(
                "Dil değiştirildi...",
                "Language changed...",
                "Язык изменён...",
                "语言已更改...",
                "Sprache geändert...",
                "Langue changée...");
    }

    // Support button text
    public String getSupport() {
        return getString(
                "Destek",
                "Support",
                "Поддержка",
                "支持",
                "Unterstützen",
                "Soutenir");
    }
}
