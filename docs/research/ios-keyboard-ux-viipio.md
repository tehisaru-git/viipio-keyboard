# iPhone’i vaikeklaviatuur, Androidi Gboard ja Viipio: kasutuskogemuse uuring

**Uuritud 24.11.2026. Staatus: uuring ja teostatavushinnang, mitte teostatud funktsioonide ega tulevase väljalaske lubadus.** Võrdlus tähendab Apple’i iPhone’i sisseehitatud klaviatuuri ja **Androidi Gboardi**, mitte Gboardi iOS-i versiooni. iPadi erijuhud on eraldi märgitud. Apple’i ja Google’i juhendid tõendavad kirjeldatud toiminguid; ajakirjanike ja kasutajate hinnangud on üksikkogemused, mitte mõõdetud eelistuste osakaal. Viipio seis on hinnatud `v0.10.3` aktiivse `ViipioInputMethodService`-i lähtekoodi, mitte päritud CleverKeysi funktsiooninimede põhjal. Ühe Android 15 emulaatori Gboardi katses ei avanenud tühiku allhoidmisel kogu klaviatuuri hõlmavat puuteplaati; see üks katse ei tõesta kõigi Gboardi versioonide puudumist.

## Mida iOS-i klaviatuuri juures kiidetakse?

1. **Täpset kursorijuhtimist ilma tekstile näpuga osutamata.** iPhone’il muutub tühiku allhoidmisel kogu klaviatuur puuteplaadiks: sõrmega saab kursorit vabalt liigutada ja teise sõrmega tekstivalikut laiendada.[4] TidBITSi autori kirjelduses oli funktsioon avastamata kasutajale üllatavalt kasulik; sama loo kommentaarides nimetati seda ka vahel ebakindlaks.[21] Android Police’i autori 2026. aasta kuuajases isiklikus võrdluses tundus just neljas suunas kursoriliigutus Gboardi rõhtsest tühikuviipest mugavam.[9] Need on kasutajakogemused, **mitte** sõltumatu kiiruse või täpsuse võrdlus.
2. **Paranduse nähtavust ja tagasivõtmist.** iOS märgib automaatselt parandatud sõna ajutiselt alla; seda puudutades saab algse kirjapildi tagasi valida.[4][7]
   See annab kasutajale selge vastuse küsimusele „mida klaviatuur minu eest muutis?”. Apple’i hinnang paranenud ennustustäpsusele on tootja väide.[16] The Guardiani käsitletud kaebused näitavad, et kasutajad kogevad ka ekslikke parandusi; Apple’i mudelit ei saa seetõttu pidada tõendatult Gboardist paremaks.[11]
3. **Kasutaja määratud tekstiasendust.** Lühendi sisestamisel asendub see iOS-is määratud sõna või fraasiga; isiklik sõnastik saab iCloudi abil Apple’i seadmete vahel sünkroonis püsida.[6] Üks 2025. aasta arvustaja kiitis eriti seda, et asendus teeb täpselt kasutaja valitud asja ega pea ära arvama, mida ta kirjutada tahtis.[23] See on selle autori hinnang, mitte laiem kasutajauuring.
4. **Väikest, järjekindlat sisestusvoogu.** Toetatud keeltes võib halli tekstijätku tühikuga kinnitada või edasi kirjutades tagasi lükata; QuickPath lubab ühes lauses puudutusi ja sõnaviibeid segada.[5][1]
   Olemas on ühe käe paigutus ja valikuline klahvihaptika.[1] iPadi mõnes paigutuses annab klahvilt allaviibe selle väikese numbri või sümboli; iMore’i autori meelest on see eriti sujuv, kuid see **ei ole** iPhone’i üldine klahviomadus.[22] Need on töövoo ja kujunduse tähelepanekud, mitte tõend, et Apple kirjutaks kiiremini kui Gboard.

**Vastukaal:** ühes kuuajalises võrdluses hinnati Gboardi inglise ja hinglish’i kooskasutust Apple’i omast paremaks ning Apple’i automaatparandust ebajärjekindlaks.[10] Ka The Guardian dokumenteeris iPhone’i kasutajate pahameelt ekslike paranduste üle.[11] „iOS-i klaviatuuri kiidetakse” ei tähenda üleüldist üksmeelt ega eestikeelse sisestuse paremat kvaliteeti.

## Mis on Androidi Gboardis päriselt teisiti?

- **Puuteplaadi žest on erinev, mitte kursorifunktsiooni täielik puudumine.** Google dokumenteerib tühikul vasakule/paremale libistamise.[2] Google’i eraldi *Text Editing* vaade sisaldab kursori-, valiku-, lõikamis-, kopeerimis- ja kleepimisnuppe.[19] See ei ole Apple’i „hoia tühikut, liiguta kogu klaviatuuril kahes mõõtmes ja vali teise sõrmega” töövoog.[4] Android Police’i 2026. aasta autor kinnitab oma võrdlusseadmetes seda erinevust; kohalikus ühes emulaatorikatses ei ilmunud tühiku allhoidmisel Apple’i-laadset kogu klaviatuuri puuteplaati.[9] Me ei teinud kontrollitud mitmerealise teksti valiku- ega Gboardi kõigi versioonide testi.
- **Parandust saab Gboardis tagasi võtta, kuid dokumenteeritud tee erineb.** Google ütleb, et automaatse paranduse võib kohe Backspace’iga tagasi võtta; piiratud *Proofread / Fix it* parandusel on eraldi Undo.[12] Apple’i dokumenteeritud žest on **juba parandatud, ajutiselt allajoonitud sõna** puudutamine ja algse sõna valimine.[4]
  Google’i punane joon veaga sõna all ei ole sama nähtav olek.[12][4] Allikatest **ei saa kindlalt järeldada**, et ühelgi Gboardi versioonil pole samalaadset allajoonimise/puudutamise liidest.
- **Fraasilühendid on mõlemal, kinnitamine võib erineda.** Google’i Gboardi isiklikus sõnastikus võib lühendiga seotud fraas ilmuda kandidaadiribale puudutamiseks; Apple’i kirjeldatud iOS-i tekstiasendus rakendub lühendi järel automaatselt.[20][6]
  Ka üks Gboardi kasutusjuhend kirjeldab fraasi sisestamiseks ettepaneku puudutamist.[18] Seega „Gboardis puudub Text Replacement” oleks vale; võimalik Viipio eristuvus on *kasutaja kontrollitav automaatne kinnitamine ja lihtne Undo*, mitte lühendite leiutamine.[17]
- **Reasisesed ennustused pole kummagi üldine keeleülene eelis.** Apple’i toetatud keeltes ilmub hall jätk tekstiväljale ning tühik kinnitab selle.[5] Google kirjeldab *Smart Compose’i* Androidi Gboardis USA inglise keele ja nimetatud sõnumirakenduste puhul.[12] Niisiis ei ole see Gboardis üleüldiselt puudu, küll aga ei tõenda see eesti keele tuge ega üldist kättesaadavust. Ka Apple hoiatab keelepiirangute eest.[5]
- **Libistamine, haptika, suur- ja sümbolitähed, ühe käe vaade ning lõikelaud ei ole kindlad iOS-i eelised.** Gboardi juhendid kirjeldavad sõnaviibet, tühikul kursorit, suurtähti ja pikalt hoitavaid märke.[2][13]
  Eraldi kirjeldatakse haptikat ning kinnitatud lõikelauakirjeid.[24][25] Google’i Androidi juhend kirjeldab ka klahvi ülesviibatud numbrit/sümbolit; seepärast ei saa iPadi *Key Flicks’i* kui ideed nimetada Gboardis puuduvaks.[20] Võrdle konkreetse paigutuse ja keele töövoogu, mitte funktsiooni nime.

**Miks erinevus tekib?** Google ega Apple ei avalda nende võrdluste põhjal põhjust, miks Gboard ei kasuta just Apple’i kogu klaviatuuri hõlmavat puuteplaadi žesti. Dokumenteeritud fakt on **erinev disain**: Gboardi rõhtne tühikuviibe ja eraldi redigeerimisvaade versus Apple’i tühiku allhoidmine ja teise sõrme valik.[2][19][4] Hüpoteesid nagu žestikonflikt, vähene nõudlus või patenteeritud lahendus **ei ole kontrollitud põhjused**. Androidi `InputConnection.setSelection` võimaldab IME-l kursorit/valikut muuta; üldist tehnilist võimatust ei saa väita.[14] Täpset Apple’i rakendusesisest ajutist allajoonimist ei saa aga iga võõra Androidi tekstivälja peale ainult klaviatuuri joonistusvaatega tagada: tekstivälja kuvab hostrakendus ja eri editorid võivad ühenduse käske tagasi lükata või ümber tõlgendada.[3][14]

## Eestikeelne piir on tähtsam kui platvormi müüt

Apple’i hetkel avaldatud funktsioonitabel sisaldab **Estonian** klaviatuuri keeletoe ja automaatparanduse loendis, kuid mitte *Predictive Typing*, *Inline Predictions*, *Multilingual Typing* ega *QuickPath* loendis.[8] Google’i Gboardi keeltekataloog sisaldab samuti Estonian’it, kuid see nimekiri **üksi** ei kinnita eestikeelset sõnaviibet ega Smart Compose’i.[26][12] Viipio aktiivne teenus toetab eraldi ET/EN vaateid ja ET sõnaviibet; olemasolu ei tõenda päris sõrmega viibete täpsust. Seega tasub Viipio eristuvust mõõta eestikeelsete sõnade, käänete, parandamisvaeva ja sisestusväljadega, mitte väita, et Apple’i ingliskeelse mudeli koopia annaks sama kvaliteedi.

## Mida Viipios saab ja tasub teha?

**1. Kõige väiksem kasulik samm: selgelt nähtav, ajaliselt piiratud „Võta tagasi” pärast kasutaja valitud alternatiivi.** Viipio v0.10.3 näitab sõnaviibe/käsitsi tippimise kandidaate ja valitud sõna asendatakse juba kontrollitud `InputConnection`-i kaudu; praegune Backspace’i ühe vajutuse variant võib IME-le kuuluva sõnaviibe sõna kustutada, kuid puudub üldine nähtav algse kirjapildi tagasivalik.[27][29] Alustada võiks **ainult kasutaja enda valitud asenduse** lühiajalise Undo-toiminguga, mitte sisse lülitada automaatparandust. Säilitada täpne vana/uue sõna, välja, kursori ja ühenduse omand; kui editor muudab midagi või kirjutamine nurjub, ei tohi tagasivõtmine midagi pimesi üle kirjutada. Kuvada toiming oma kandidaadiribal, mitte lubada võõra rakenduse tekstis Apple’i allajoonimist.[3] See on **madalam risk** kui uus automaatparanduse mootor, kuid nõuab üksikteste ja päris editori katseid. Allika-asukohad: `ViipioInputMethodService.kt:1112–1149,1154–1209,1725–1791`; `swipeui/ManualCompletions.kt:109–136`.

**2. Eraldi valitav tekstilühendite laiendamine.** Viipio aktiivses teenuses pole iOS-i moodi „kirjuta lühend → täispikk fraas” töövoogu tõendatud; päritud CleverKeysi `DynamicTemplate.kt` olemasolu ei tähenda, et Viipio seda kasutaks.[27][30] Lubada ainult kasutaja enda loodud ET/EN lühendeid, selge eelvaate või Undo-ga ja algul vaikimisi väljas; välistada paroolid, PIN, URL, e-post, `NO_SUGGESTIONS` ja incognito ning hoida salvestatud fraasid üksnes seadmes. Enne automaatset asendust nõuda täpne lühend + sõnapiir, muutumatu valik ja `InputConnection`-i readback; defineerida kokkupõrked tavaliste sõnadega ja varundus/ekspordi privaatsus.[14] **Keskmine risk**: ka kasutaja määratud reegel võib sobivas kontekstis ekslikult käivituda. Allika-asukohad: `ViipioInputMethodService.kt:1939–1960`; `ui/settings/SettingsScreen.kt:114–241`; `swipeui/ManualCompletions.kt:109–136`.

**3. Apple’i-laadne kursori puuteplaat: esmalt väike katse, alles siis funktsioon.** Aktiivse Viipio `SurfaceGesture`-i loendis puudub kursorirežiimi sündmus; tühik annab tavalise puudutuse.[28][27] Katse võiks käsitleda tühiku *allhoidmist* eraldi olekuna, jätta lühike tühik puutumata, kuvada režiim selgelt ning alustada ühe sõrmega kursorist. Alles pärast toimivat ühe- ja mitmerealist editori kontrolli lisada teise sõrme tekstivalik; tühik, keelevahetus, sõnaviibe, Backspace’i hoidmine ja kandidaadid ei tohi žestiga konflikti minna. `InputConnection.setSelection` on võimalik, kuid ei taga kõigi Androidi rakenduste identset valiku- või kerimiskäitumist.[14] **Keskmine kuni kõrge risk**, eriti valiku ja Compose’i/rikaste editorite korral. Allika-asukohad: `estonianui/SurfaceGestureController.kt:5–12,21–76`; `ViipioInputMethodService.kt:1738–1781`.

**4. Eestikeelne järgmise sõna või reasisene ennustus: uurimine, mitte kohe lubadus.** Kandidaadiriba on Viipios olemas, kuid hall tekst *võõra rakenduse tekstivälja sees* ja tühikuga kinnitamine on eri taseme funktsioon.[27][5] ET/EN eraldi valik, privaatsus, kohaliku mudeli latentsus/mälu ning kirjutamise tagasilükkamine vajavad mõõtmist. Eestikeelse asjakohase kandidaadi kuvamine oma ribal on kontrollitavam esimene samm kui Apple’i visuaalselt identse inline-ennustuse lubamine. Apple’i ja Google’i keelepiirangud annavad võimaluse, kuid **mitte** tõendit, et meie mudel oleks parem.[8][12] Allika-asukohad: `ViipioInputMethodService.kt:360–394`; `swipeui/ManualCompletions.kt:37–49,109–136`.

**Mida mitte kopeerida:** iOS suunab turvalise paroolivälja kolmanda osapoole klaviatuurilt süsteemiklaviatuurile; see on **iOS-i platvormireegel**, mitte Viipio Androidi IME-s sisse lülitatav valik.[15] Android lubab IME-l parooliväljaga töötada ning nõuab oma liideses salajase teksti varjamist ja mitte talletamist.[3]
Viipio olemasolevaid tundliku välja piiranguid tuleb säilitada ning iga uut lühendi-/ennustusfunktsiooni testida parooli, PIN-i, `NO_SUGGESTIONS`, `NO_PERSONALIZED_LEARNING` ja incognito korral.[27][3] Apple’i iCloudi automaatne sünkroon ega süsteemi OTP/paroolitäite usaldusahel ei ole Viipio võrguühenduseta klaviatuuri realistlikud lubadused.

**Enne mis tahes teostusotsust:** teha väike ET/EN kasutuskatse päris telefonis sama redigeerimisülesande ja võrdsete sisenditega; mõõta kursori parandamise puudutusi, vale asenduse ennistamise samme, sobiva lühendi kasu, juhuslikke asendusi ja editori tagasilükkamisi. Eraldi kontrollida TalkBacki, suuremat fonti, landscape’i, rakendusevahetust ning väljade piirangud. Selliseid tulemusi käesolevas uuringus **ei mõõdetud**. Uuring ei muuda Viipio klaviatuuri koodi ega avalda uut APK-d.

## Sources

[1] https://support.apple.com/guide/iphone/type-with-the-onscreen-keyboard-iph3c50f96e/ios
[2] https://support.google.com/gboard/answer/2842292?hl=en&co=GENIE.Platform%3DAndroid
[3] https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method
[4] https://support.apple.com/guide/iphone/select-edit-and-move-text-iph1a9cae52c/ios
[5] https://support.apple.com/guide/iphone/use-predictive-text-iphd4ea90231/ios
[6] https://support.apple.com/guide/iphone/use-shorthand-for-frequently-used-text-iph6d01d862/ios
[7] https://support.apple.com/en-us/104995
[8] https://www.apple.com/ios/feature-availability
[9] https://www.androidpolice.com/switched-to-iphone-realized-gboard-runs-terribly-on-android
[10] https://www.androidpolice.com/tried-google-keyboard-samsung-keyboard-apple-keyboard-month
[11] https://www.theguardian.com/technology/2025/nov/01/ducking-annoying-why-has-iphones-autocorrect-function-gone-haywire
[12] https://support.google.com/gboard/answer/7068415?hl=en&co=GENIE.Platform%3DAndroid
[13] https://support.google.com/gboard/answer/2811346?hl=en&co=GENIE.Platform%3DAndroid
[14] https://developer.android.com/reference/android/view/inputmethod/InputConnection
[15] https://developer.apple.com/documentation/uikit/configuring-a-custom-keyboard-interface
[16] https://www.apple.com/newsroom/2023/09/ios-17-is-available-today
[17] https://www.androidpolice.com/gboard-personal-dictionary-add-edit-words
[18] https://www.howtogeek.com/gboard-has-tons-of-hidden-tricks-you-probably-arent-using
[19] https://blog.google/products-and-platforms/products/search/gboard-android-gets-new-languages-and-tools
[20] https://www.android.com/intl/ja_jp/articles/460
[21] https://tidbits.com/2024/08/31/tipbits-use-trackpad-mode-when-editing-text-on-the-iphone-and-ipad
[22] https://www.imore.com/ipad/this-ipad-keyboard-trick-is-the-quickest-way-to-type-numbers-and-symbols-so-why-do-more-people-not-use-it
[23] https://www.makeuseof.com/time-saving-iphone-text-trick
[24] https://support.google.com/gboard/answer/6102154?co=GENIE.Platform%3DAndroid&hl=en
[25] https://support.google.com/gboard/answer/10742542?co=GENIE.Platform%3DAndroid&hl=en
[26] https://support.google.com/gboard/answer/6380730?co=GENIE.Platform%3DAndroid&hl=en
[27] https://github.com/tehisaru-git/viipio-keyboard/blob/94030166cb1b2def64f6dfa46df01030beaee723/src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt
[28] https://github.com/tehisaru-git/viipio-keyboard/blob/94030166cb1b2def64f6dfa46df01030beaee723/src/main/kotlin/tribixbite/cleverkeys/estonianui/SurfaceGestureController.kt
[29] https://github.com/tehisaru-git/viipio-keyboard/blob/94030166cb1b2def64f6dfa46df01030beaee723/src/main/kotlin/tribixbite/cleverkeys/swipeui/ManualCompletions.kt
[30] https://github.com/tehisaru-git/viipio-keyboard/blob/94030166cb1b2def64f6dfa46df01030beaee723/src/main/kotlin/tribixbite/cleverkeys/ui/settings/SettingsScreen.kt
