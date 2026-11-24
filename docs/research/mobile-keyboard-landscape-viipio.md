# Mobiiliklaviatuuride võrdlus ja Viipio võimalused

**Seis: 24.11.2026.** Uurisin tootjate dokumente, avalikke rakendusepoe kirjeid ja avatud projektide juhendeid. Need kirjeldavad pakutavaid omadusi, **mitte mõõdetud sisestustäpsust, viiteaega ega kasutajate rahulolu**. iOS-i ja Androidi sama nimega rakendusi ei tohi pidada funktsioonilt identseteks: Gboardi kaks poe kirjet kirjeldavad eri platvorme.[9][15] Viipio praegune avalik versioon on Androidi võrguühenduseta ET/EN-klaviatuur, millel on puudutus- ja viipesisestus, kohalikud soovitused, isikliku sõna käsitsi lisamine ning eraldi nõusolekuga diagnostika ([praeguste omaduste kirjeldus](../../README.md)). See dokument ei lisa rakendusele funktsioone ega uut APK-d.

## Populaarsus: mida saab päriselt võrrelda?

Google Play kuvatud allalaadimiste **kumulatiivne lävend** oli vaatluse ajal Androidi Gboardil **10 mld+**[15], SwiftKeyl **1 mld+**[14] ja Typewise'il **1 mln+**[16]. Need ei ole aktiivsete inimeste arvud; eelpaigaldus ja muud poed moonutavad võrdlust. Kontrollitud App Store'i kirjed ei kuva samaväärset avalikku paigaldusmõõdikut.[9][8] Seepärast tähendab „populaarne” siin neid laialt allalaaditud Androidi tooteid; ülejäänud on **huvitavad teistsuguse lähenemisega näited**, mitte turuosade pingerida. Kõik poenäitajad olid USA/ingliskeelsel avalikul lehel vaatluse hetkel ja võivad muutuda.

## Laiatarbelised klaviatuurid

- **Gboard — Android ja iOS.** Androidi kirjeldus pakub sõnaviibet, mitmekeelset sisestust, dikteerimist, tõlget ja kohandatavat kasutusviisi.[15] iOS-i kirje rõhutab ka Google'i otsingut klaviatuurilt, kuid USA poe viimane nähtav versioon pärineb **2022. aastast**; seega ei tõesta Androidi uus omadus selle olemasolu iPhone'is.[9] **Viipiole:** keelevaliku ja paranduse kiire töövoog; mitte veebiteenuste/GIF-otsingu kopeerimine kui sisestuskvaliteedi eeltingimus.
- **Microsoft SwiftKey — Android ja iOS.** Fookus on isikupärastatud sõnaennustusel, viipesisestusel ja keelte kooskasutusel.[14][8] Microsofti *Androidi* keeleloendis on **Estonian** ja korraga saab sisse lülitada kuni viis keelt; loend ei mõõda eesti keele viipe täpsust ega kinnita sama iOS-i funktsioonivalikut.[33] **Viipiole:** kandidaatide ET/EN-valiku jälgitavus; kontopõhine sünkroonimine ja võrgupõhine AI ei ole praeguse võrguvälise mudeli jaoks vaikimisi sobivad.
- **Samsung Keyboard — Galaxy Android.** Eristuvad suuruse muutmine, **ühe käe, hõljuv ja poolitatud režiim**, kohandatavad sümbolid ning seadmega lõimumine.[20] *Writing assist* pakub valitud Galaxy seadmetes stiili-, grammatika- ja tõlkeabi; selle Samsungi avaldatud keelte loendis **eesti keelt ei ole** ja seadmete tugi on piiratud.[34] **Viipiole:** kitsamal ekraanil kasutaja valitav paigutus; mitte One UI-sõltuv AI.
- **Typewise — Android ja iOS.** Alternatiivne kuusnurkne paigutus ja suuremad klahvid, suunaviiped suurtäheks, kustutuseks ning taastamiseks; iOS-i kirje loetleb ka **eesti keele** ja ütleb, et tavaline sisestus ei nõua *Full Access'i*.[16][30]
  „Neli korda vähem kirjavigu” on tootja väide, mitte siin sõltumatult kontrollitud tulemus.[30]
  iOS-i poe enda privaatsussildil on samal ajal märgitud võimalikke jälgimis- ja kasutusandmeid: klaviatuuri andmeid ning rakenduse muu osa statistikat ei tohi segi ajada.[30] **Viipiole:** puuteala ja taastamisžesti katse, mitte QWERTY sunniviisiline asendamine.
- **Grammarly — iOS-is klaviatuur, Androidis praegu kirjutamisabiline.** iPhone'i tootekirje pakub õigekirja-, stiili-, tooni- ja AI-ümberkirjutuse abi.[45] Androidi praegune Play kirjeldus ütleb sõnaselgelt, et teenus töötab teiste klaviatuuridega (nt Gboard/SwiftKey), ning kirjeldab ligipääsetavusloa kasutamist teksti töötlemiseks; seda ei ole õige esitada Androidi võrguühenduseta IME-na.[44] **Viipiole:** selgesõnaliselt valitav parandustoiming, mitte pidev pilvetöötlus.

## Omanäolised Androidi klaviatuurid ja privaatsus

- **FUTO Keyboard.** Kohalik viipesisestus ja häälsisestus, seadistatav tühikuviibe ning keele- ja mudelivalik.[10][11][42]
  Tootja privaatsuspoliitika järgi Androidi klaviatuur **ei küsi võrguluba**; mudeli/sõnastiku allalaadimine avab brauseri ning kasutaja saadetud veateade võib sisaldada isiklikke andmeid.[43]
  Dokumentatsiooni järgi peab paljude keelte sõnastiku eraldi laadima. Kohalik Transformer-ennustus töötab praegu **ainult inglise keeles**, seega see konkreetne funktsioon ei ole ET jaoks saadaval; sõnastikupõhine ET-sisestus on eraldi asi.[42][23]
  Rakenduse **FUTO Source First** litsents piirab muudetud koodi kasutust ja levitamist, seega see *ei ole vabalt ülevõetav avatud lähtekoodina*.[27] **Viipiole:** valitava tühikuviibe ja võrguvälise ressursivoo muster; mitte võõra mudeli/koodi kopeerimine.
- **HeliBoard.** AOSP/OpenBoardi põhjal kohandatav võrguühenduseta Androidi klaviatuur: teemad, imporditavad sõnastikud, isikustatud sõnade kustutamine ning varundus/taastamine.[13][35]
  Allikate järgi on eesti *eksperimentaalne põhisõnastik*, mis ei tõenda head ET-viibete tuvastust.[48]
  Projekti README ütleb otse, et viipesisestus vajab **eraldi suletud lähtekoodiga teeki, mida rakendusega kaasa ei anta**.[13] **Viipiole:** selgita soovituse allikas ja anna oma sõnadele ülevaatus-/kustutamisvõimalus; ära impordi suletud viipeteeki vaikimisi.
- **FlorisBoard.** Teemad, paigutused ja laiendused on privaatsusele suunatud Androidi beetaprojekti fookus; lähtekoodis on **õ/ä/ö/ü-ga eesti paigutus**.[26][47]
  README ütleb, et **praegustes väljalasetes puuduvad sõnasoovitused ja õigekirjakontroll**; teekaardis on nende ja viipemootori arendus tulevikuks.[26][21]
  ET paigutus ei ole tõend ET sõnastikust ega ET sõnaviipest.[47][26] **Viipiole:** väike arv selgeid kohandusi enne täit teema- ja laiendussüsteemi.
- **AnySoftKeyboard.** Väliste keelepakkidega klaviatuur pakub viibet, sõnaennustust, seadistatavat automaatparandust ja inkognito-režiimi; projekt kirjeldab põhiklaviatuuri internetiühenduseta.[37] Kontrollitud ametlikust keelepakkide loendist ei leidnud eesti pakki[38] — see on *loendi piir*, mitte tõend, et eestikeelne sisestus on muul viisil võimatu. **Viipiole:** testida, kas ET ja EN saavad koos olla ilma kogemata vale keele sõnu pakkumata; pakisüsteemi ehitamine ei ole kohe vajalik.
- **Thumb-Key.** Pöidlale optimeeritud **3×3** klahviruudustik: üksiktähed valitakse suunaviibetega, lisaks tühikul kursor, tagasilükkel sõnakustutus ja muud töötlusžestid.[19] See **ei ole** üle klahvide tõmmatud sõna tuvastus ega Viipio praeguse sõnaviibe asendus. **Viipiole:** ühe funktsiooniga valikuline kursorižest, mitte uue 3×3 baasklaviatuuri õpetamine.
- **Unexpected Keyboard.** Programmeerimisest alguse saanud klahvinurga suunaviiped avavad sümboleid; olemas on ET-paigutuse fail.[17][41]
  Erinevalt Viipio praegusest APK-st sisaldab kontrollitud lähtekoodi manifest siiski **INTERNET-luba**; poolelioleva muudatuse eesmärk on panna sõnastiku allalaadimine brauserisse, et luba eemaldada.[28][29]
  Avatud muudatus ei tõenda, et see juba väljalastud APK-s puudub.[29] **Viipiole:** vabatahtlik sümboliviibe alles siis, kui sõnaviipest lahus katsetatud; puudutussümbolite pärast pole põhjust üldist võrguluba võtta.

**Fleksy kui ajalooline žestinäide, mitte tänane soovitus.** Selle tarbijarakenduse Androidi Play link andis kontrollimisel „Not Found” ning varasem USA iOS-i kirje HTTP 404; konkurendi 2026. aasta kirjutis kirjeldab veebisaidi/SDK kadumist.[3][51][36] See **ei tõesta** eemaldamist igas piirkonnas. Varane sõltumatu ligipääsetavuse ülevaade kirjeldab vasakviibet kustutamiseks ja paremviibet tühikuks.[50] Need on uuritavad ajaloolised töövõtted, kuid Fleksy praegusele saadavusele või SDK-le ei tohi Viipio plaani rajada.

## iOS-i erinäited, mida Androidi Viipios saab tõlgendada

- **Phraseboard:** korduvvastused on klaviatuuris teemade kaupa järjestatud; poe kirjeldus nõuab kasutajafraaside näitamiseks iOS-i *Open Access'i*.[31] See tõendab fraasikogu töövoo olemasolu, mitte selle kvaliteeti ega aktiivset arendust.
- **TextExpander:** iPhone'i klaviatuuris ilmuvad sisestatud lühendiga sobivad tekstijupid soovitusribale; tootja järgi võivad ka **viipega sisestatud lühendid** fraasiks laieneda.[32] See vajab kontot ja iOS-is *Full Access'i*; Androidis töötab TextExpander **teise klaviatuuri kõrval ligipääsetavusteenusena**, mitte sama IME-na.[32][40] Viipio saab proovida ainult selge kinnitusega **kohalikke** ET/EN-fraasilühendeid, ilma kontota ja ilma automaatse laienduseta tundlikes väljades.

**iOS-i süsteemipiir:** Apple asendab kolmanda osapoole klaviatuuri salajastes ja mõnes telefoninumbriväljas süsteemiklaviatuuriga; rakendus võib kolmanda osapoole klaviatuuri ka keelata.[24] *Allow Full Access* annab võimaluse võrgu- ja jagatud andmete kasutuseks, **mitte tõendi, et konkreetne klaviatuur saadab sisendit võrku**.[25] Viipio on Androidi rakendus: need iOS-i piirid ega rakenduste töövõtted ei kandu Androidi üks ühele.

## Viipio soovitatav järjekord

1. **Põhifunktsiooni kvaliteet enne lisasid.** Mõõta ET/EN sisestuse sõnaviibe täpsust, paranduse leidmise kiirust ja viidet päris seadmel. Avalik [README](../../README.md) märgib, et senised sünteetilised emulaatorikatsed neid ei tõesta. Lisada **kasutaja valitud soovitusega tehtud asenduse nähtav tagasivõtt**: soovitused ei asenda praegu teksti automaatselt ning olemasolev **+ Add → Undo** käib isikliku sõna lisamise kohta. Piirata tagasivõtt sama välja ja värske muutusega; mitte lugeda/taastada hostrakenduse juba muudetud või tundlikku teksti ([eelmine UX-uuring](ios-keyboard-ux-viipio.md)).
2. **Väike valikuline kursoriviibe tühikuklahvil.** FUTO ja Thumb-Key demonstreerivad eri juhtimisviise.[11][19] Viipio [aktiivne žestikontroller](../../src/main/kotlin/tribixbite/cleverkeys/estonianui/SurfaceGestureController.kt) eristab praegu täheviibet ja tagasilükkest algavat sõnakustutust; lisandus vajab konflikti- ja kiirusteste, nähtavat väljalülitust ning parooli-, numbrivälja ja mitme Androidi tekstiredaktori katseid. Kui need ebaõnnestuvad, jätta senine tühik muutmata.
3. **Fraasilühendid ja sõnade läbipaistvus.** Eraldi kasutaja loodud ET/EN fraas, lühendi täpne vaste ja **kasutaja puudutusega kinnitatav** soovitus; välista salajased väljad ja väldi peidetud automaatset asendust.[32][31] Samasse väikesesse tööpaketti sobib sõnaraamatu ülevaatus, selgesõnaline kustutamine ja vajaduse korral kohaliku varukoopia kasutaja käe läbi.[35] Praegused [Viipio käsitsi soovitused](../../src/main/kotlin/tribixbite/cleverkeys/swipeui/ManualCompletions.kt) on piiratud sõnastiku- ja kirjaveaotsingud, mis arvestavad isiklikke sõnu ning võivad kasutada **staatilist kontekstipõhist ümberjärjestamist**; see ei ole automaatselt õpitud isiklik ennustusmudel.
4. **Ergonoomika ainult vajaduse tõendamisel.** Piiratud kõrguse või ühe käe režiimi katse Samsungi eeskujul: ET eritähte, sõnaviibe geomeetria, kitsad/ülisuured ekraanid ja ligipääsetavus tuleb eraldi läbi proovida.[20] Kuusnurkse või 3×3 põhipaigutuse asendamine pole praegu proportsionaalne.

**Mitte kohe:** kohustuslik konto, pilvekirjutaja/-tõlge, vaikimisi salvestuv lõikelauaajalugu, pakendatud suletud viipeteek või mujalt üle võetud sõnaloend. Need muudaksid õigusi, privaatsuslubadust, hoolduskulu või litsentsikohustusi. Uurida võib teiste töövoogu; koodi, varade, mudelite ja sõnastike kasutamine vajab eraldi päritolu- ja litsentsikontrolli ([Viipio LICENSE](../../LICENSE), [NOTICE](../../NOTICE)).

## Sources

[3] https://play.google.com/store/apps/details?id=com.syntellia.fleksy.keyboard&hl=en
[8] https://apps.apple.com/us/app/microsoft-swiftkey-ai-keyboard/id911813648
[9] https://apps.apple.com/us/app/gboard-the-google-keyboard/id1091700242
[10] https://keyboard.futo.tech
[11] https://docs.keyboard.futo.tech/gestures/spacebar
[13] https://github.com/HeliBorg/HeliBoard
[14] https://play.google.com/store/apps/details?id=com.touchtype.swiftkey&hl=en&gl=US
[15] https://play.google.com/store/apps/details?id=com.google.android.inputmethod.latin&hl=en&gl=US
[16] https://play.google.com/store/apps/details?id=ch.icoaching.typewise&hl=en&gl=US
[17] https://github.com/Julow/Unexpected-Keyboard
[19] https://github.com/dessalines/thumb-key
[20] https://www.samsung.com/us/support/answer/ANS10001592
[21] https://github.com/florisboard/florisboard/blob/main/ROADMAP.md
[23] https://docs.keyboard.futo.tech/settings/textprediction
[24] https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/CustomKeyboard.html
[25] https://developer.apple.com/documentation/uikit/configuring-open-access-for-a-custom-keyboard
[26] https://github.com/florisboard/florisboard
[27] https://github.com/futo-org/android-keyboard/blob/master/LICENSE.md
[28] https://github.com/Julow/Unexpected-Keyboard/blob/master/AndroidManifest.xml
[29] https://github.com/Julow/Unexpected-Keyboard/pull/1510
[30] https://apps.apple.com/us/app/typewise-custom-keyboard/id1470215025
[31] https://apps.apple.com/us/app/phraseboard-keyboard/id916976066
[32] https://apps.apple.com/us/app/textexpander/id6805960547
[33] https://support.microsoft.com/en-us/swiftkey-keyboard/what-languages-are-currently-supported-for-microsoft-swiftkey-keyboard
[34] https://www.samsung.com/us/support/answer/ANS10000943
[35] https://github.com/HeliBorg/HeliBoard/wiki/FAQ
[36] https://keyboardkit.com/blog/2026/06/08/fleksy-shuts-down-their-website
[37] https://github.com/AnySoftKeyboard/AnySoftKeyboard
[38] https://github.com/AnySoftKeyboard/AnySoftKeyboard/blob/main/addons/languages/PACKS.md
[40] https://textexpander.com/mobile
[41] https://github.com/Julow/Unexpected-Keyboard/blob/master/srcs/layouts/latn_qwerty_et.xml
[42] https://docs.keyboard.futo.tech/settings/languagesmodels
[43] https://keyboard.futo.tech/privacy
[44] https://play.google.com/store/apps/details?id=com.grammarly.android.keyboard&hl=en_US&gl=US
[45] https://apps.apple.com/us/app/grammarly-ai-keyboard-voice/id1158877342
[47] https://github.com/florisboard/florisboard/blob/main/app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/characters/estonian.json
[48] https://codeberg.org/Helium314/aosp-dictionaries/raw/branch/main/README.md
[50] https://afb.org/aw/13/11/15869
[51] https://apps.apple.com/us/app/fleksy-gif-web-yelp-search/id520337246
