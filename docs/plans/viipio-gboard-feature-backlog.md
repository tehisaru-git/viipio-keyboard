# Viipio: Gboardi funktsioonide uuring ja järjestatud arendusplaan

**Staatus:** uurimis- ja planeerimisdokument, mitte v0.6.0 väljalaskelubadus. **Alus:** Google'i Androidi Gboardi juhendid [1]–[9], 13.11.2026 ühiskasutuses emulaatoris tehtud nähtava liidese vaatlus (O1), Viipio nõuete spetsifikatsioon `docs/specs/estonian-swipe-ui.md` (V1) ja avaldatud `docs/releases/v0.5.1.md` (V2). Eelmise `viipio-gboard-parity.md` versiooninumbrid ja lähtekoodi reanumbrid on ajaloolised ettepanekud; käesolev dokument kirjeldab **uut prioriteetide järjekorda**, mitte väidet, et kõik vana plaani funktsioonid on juba olemas. Poolik v0.6.0 tööharu ei ole paigaldatud väljalase.

## Mida saab allikatest järeldada

- Google kirjeldab sõna sisestamist sõrmega tähtedelt tähtedele libistades ning sõna asendamist soovitusega. See ei avalda dekoodrit, Estonian top-1 täpsust ega Gboardi mudelite litsentsi.[1] Google'i abileht kirjeldab ka tühikuklahvil kursoriliigutust, topelt-Shift'i Caps Lock'i, klahvi hoidmist diakriitikute jaoks, emojide korduvat valikut ja keele vahetamist.[2][6]
- Google kirjeldab soovitusriba, soovituse eemaldamist pika hoidmisega ja sõna lisamist sõnastikku punase allajoonimise kaudu; „Fix it” tekstiparandus on abilehe järgi US Englishi piiranguga. Lehel on osade soovituste keele- ja seadme-eeldused: **ärge laiendage neid väiteid automaatselt eesti keelele**.[3]
- Gboardi juhend kirjeldab lõikelaualt valikulist kleepimist, hiljutiste kirjete kadumist ja kinnitatud kirjete erandit.[4]
- Teemad, klahviheli/vibratsioon ja emoji suurus on seadistatavad; vaikimisi emoji suurus järgib seadme fondisuurust.[5]
- Käekirja ja tõlke funktsioonid on eraldi ning käekiri pole kõigis keeltes saadaval.[7][8]
- **O1 — ainult selle emulaatori seadistus, mitte Gboardi üldreegel:** Androidi 720×1600 / 320 dpi emulaatoris oli Gboard valitud IME, nähtav seadistatud keel **English (UK) QWERTY**. Tühjas Android Settings'i otsinguväljas oli hele QWERTY, väikesed numbrivihjed, tööriistaribal funktsioonimenüü/lõikelaud/tõlge/seaded/mikrofon ning allservas `?123`, koma, emoji, tühik, punkt ja otsing. Keelevahetusnupp ei olnud selles *ühes* vaates nähtav. Seadete menüü oli ingliskeelne; vaadeldi kategooriaid Languages, Preferences, Theme, Corrections and suggestions, Glide typing, Voice typing, Write in text fields, Clipboard, Dictionary, Emojis/stickers/GIFs ja Privacy. Nähtavatest seadetest **ei järeldatud nende sisse-/väljalülitatud olekut**. Proovimise järel jäi Gboard aktiivseks ja avakuva taastati. Vaatlusfailid `build/tooling/gboard-study/{observations,details,preferences-all}.json` ja kohalik kuvatõmmis on arenduskeskkonnas, mitte levitatav Gboardi kujundusvara.
- **O1 detailid:** Glide typing lehel olid liugjälg, vasakule Backspace'ilt sõnakustutus ning tühikul kursorijuhtimine. Preferences näitas numbririda, emoji/keeleklahvi valikut, ühe käe režiimi, topelttühiku punkti, automaatset tühikut kirjavahemärgi järel, klahvisümbolite hoidmist või allaviipega valimist, hääle/vibratsiooni ja klahvi hüpiknäitu; selle seadme kuvatud hoidmisviivitus oli **300 ms**. Corrections and suggestions lehel olid auto-correction, auto-capitalisation, spell/grammar check, suggestion strip, word/next-word suggestions, Smart Compose ja contacts. Emoji lehel olid kiirvalik, sümbolivaate hiljutised emoji'd, suurus, tekstist emoji soovitus ja Emoji Kitchen. Privacy lehel olid kasutusstatistika, isikupärastamise ja õpitud andmete kustutamise valikud. See on **menüüde olemasolu vaatlus**, mitte tõestus, et need töötavad eesti keeles või võrguühenduseta.

- **O2 — 14.11.2026 nähtav võrdlus, mitte universaalne Gboardi spetsifikatsioon:** eraldi ajutise Androidi testäpi `TYPE_CLASS_NUMBER | TYPE_NUMBER_VARIATION_PASSWORD` väljal näitas paigaldatud Gboard 3 × 4 suurt numbriklahvide võrku (`1–9`; alumisel real Backspace, `0`, kinnitustoiming) ja oma tööriistariba. Tavalise `TYPE_CLASS_NUMBER`, kümnend- ja märgiga kümnendvälja puhul oli 4 × 4 võrk: `1–9`, alumisel real koma, `0`, punkt ja kinnitustoiming, eraldi parem abiveerg; telefoniväljal lisaks `*`, `#`, `+` ning numbrite tähevihjed; kuupäevavälja vasakus alumises nurgas `/`, kellaajal `:`, ühisel kuupäeva/kellaaja väljal mõlema eristus (`/:`). Selle emulaatori kümnendväljas lükati Gboardi koma tagasi, punkt võeti vastu. Nii tuleb numbrivaadet kavandades eristada väljatüübi tegelikku lubatud märgistikku ja ekraanil nähtavat klahvi. Viipio joonistab oma klahvid ning ei kopeeri Gboardi ikoone ega vara.

## Prioriteedi reegel ja piirid

- **P0:** igapäevase eesti viipe- ja tavalise tekstisisestuse kvaliteet, parandatavus, töökindel ET/EN eraldus, selge paigutus ning andmekaitse. Enne uue põneva nupu lisamist peab olemasolev sisestus tööle jääma.
- **P1:** otseselt kasutaja soovitud, kuid põhisisestusest sõltuv töövoog: emojiotsing, viimistletud juhtseaded ja väiksemad sisestusmugavused.
- **P2:** kasulik võrdlusmaterjal, kuid mitte praeguse kasutaja prioriteet: lõikelaua ajalugu, ühe käe/ujuv klaviatuur, GIF, hääl, tõlge, käekiri, generatiivne korrektuur, kleepsud. Ära lisa nende pärast Interneti- ega mikrofoniõigust.
- Pingerida on **omaniku nõuetel põhinev tooteotsus**, mitte väljamõeldud Gboardi kasutussageduse statistika. Viipio peab joonistama oma klahvid, ikoonid ja paleti ning kasutama ainult õiguspäraselt levitatavat avatud dekoodrit/andmestikku. Ära kopeeri Gboardi APK-d, mudeleid, koodi, sõnastikke, fonte, ikoone ega kuvatõmmiseid tootevaraks.
- Keelevalik peab Viipios olema **eraldi ET või EN**, mitte Gboardi võimalike mitmekeelsete soovituste pime koopia. Klaviatuuri nupud ja seaded on **inglise keeles**. Automaatne lausealguse suurtäht jääb vaikimisi **OFF**; automaatparandust ei lülitata vaikselt sisse. Soovitusriba liigub horisontaalselt **ainult päris ületäitumisel**. GIF/hääl ei ole arenduse algfaasi eesmärk (V1; omaniku täpsustused).

## P0 — esmalt teostatavad, sõltumatult kontrollitavad töövood

### P0.1 Eesti viipe kvaliteet ja külmkäivitus

**Võrdlus:** Gboard liugkirjutus ja soovitusega asendamine.[1] **Viipio seis:** v0.5.1-l on ET/EN viibe ja kandidatuuriba; sõnastiku olemasolu, väike sünteetiline liugkatse ja rohelised testid ei tõesta Pixel 8 sõrme täpsust (V2). v0.6.0 käsilolev diagnostika ei tohi pidurdada tavalist viibet.

**Teostuse järjekord:** (a) kontrolli paigaldatud APK ET ja EN aktiivset klahvigeomeetriat ning sõnastikku; (b) eelsoojenda dekooder pärast mõõdetud vaate valmimist ja keelevahetust; (c) välista tulemus, kui sisestusväli, tekstivalik või väljal olev tekst on muutunud; (d) kogu opt-in etteantud viibetestist dekoodri tüüp, joondatud kandidaatide skoorid ja tegelik kinnitatud parandamine; (e) analüüsi `siis`/`sis` koos konkureerivate sõnadega, **mitte** ühe sõna globaalse kunstliku eelistamisena. Eralda külm- ja soojendatud viipe latentsus.

**Valmis, kui:** eri keelte sõnastikud ei leki; etteantud ET õ/ä/ö/ü, käänatud, lühikesed ja levinud vestlussõnad jõuavad päriselt sisestusvälja; top-1/top-3, paranduste arv, vale keele pakkumised ning p50/p95 esitatakse eraldi inimese füüsilise telefoni ja emulaatori/sünteetika kohta. Määra lävendid *enne* häälestust tegeliku lähtejoone põhjal; hoia arendus- ja kontrollfraasid lahus. Tagasilükatud või hilinenud viipe järel sisestusvälja midagi uut ei ilmu.

### P0.2 Tavaline tippimine, kandidaadid ja ettevaatlik parandamine

**Võrdlus:** Gboard pakub sõna sisestamisel soovitusi ja kasutaja saab pakutud sõna ise valida; autocorrection on eraldi seadistus.[3] **Viipio seis:** v0.5.1 pakub ET/EN käsitsi sisestuse alguse põhjal kuni kolme kandidaati; teed ei tohi nimetada Gboardi kvaliteediga veaparandajaks (V2). v0.6.0 isiklike sõnade pakkumine on alles avaldamata tööpuus.

**Teostus:** säilita vahe kandidaadi *näitamise*, käsitsi kinnitatud asenduse ja automaatse asenduse vahel. Lisa esmalt mõõdetud typo- ja diakriitikataluvus ainult selge kandidaatide kuvamisega; automaatparanduse üle otsusta eraldi ja vaikimisi OFF. Koosta üks editori-omandi kontroll käsitsi tähe, tühiku, kirjavahemärgi, kandidaadivaliku ja viipe järel. Ühe kandidaadi korral täida riba visuaalselt, kolme lühikese korral jaota võrdselt, segapikkade puhul säilita kogu sõna; kerimine üksnes siis, kui loomulikud laiused ei mahu.

**Valmis, kui:** `ter`-laadse ET ja `te`-laadse EN alguse kandidaat kuvatakse õigel keelel; teadlik puudutus asendab vaid kontrollitud aktiivse sõna, juhuslik lohistamine ei kirjuta; tagasilükatud `commitText`/valitud tekst/võõras kursorimuutus säilitab olemasoleva sisestusvälja sisu. Testi lühike sobiv rida, segapikk sobiv rida, ületäitumine, fondiskaala ja loetamatu sisestusväli; paigaldatud APK-s ei nihku numbririda ega kaota sisestusvälja nähtavusest.

### P0.3 Vigade parandamine, kursor ja sisestusväljade ühilduvus

**Võrdlus:** Gboard dokumenteerib tühiku peal kursoriliigutuse ning sõna asendamise soovitusega; O1 näitas eraldi glide-delete ja glide-cursor juhtelemente.[1][2] **Viipio seis:** v0.5.1-l on omandatud sõna tagasivõtt, Unicode'i arvestav kustutus ja sõnavalik; aktiivses Viipio teenuses ei ole Gboardi moodi tühikuklahvi kursoriloha tõestatud. Ära loe päritud CleverKeysi vaadet toimivaks Viipioks.

**Teostus:** lisa tühikul vasak/parem lohistamine vaid pärast selget suunaga distantsi; tavaline tühiku puudutus jääb puutumatuks. Tagasilükkeklahviga sõna kustutuse žest ei tohi peita üksikmärgi, pikalt hoidmise ega valitud teksti kustutust. Kui asendad varasema sõna, nõua sama sisestusvälja, kokkukukkunud kursori ja täpselt valitud omatud sõna võrdlust; muul juhul ära kirjuta. Enne Enterit ja kirjavahemärki kinnita veel omatud viipesõna. Compose'i ja tavalise tekstivälja käitumine võib erineda.

**Valmis, kui:** kahe järjestikuse viipe järel tühik/kirjavahemärk, alternatiiv, Undo ja Backspace töötavad; tühiku lohistamine liigutab kursorit, kuid ei sisesta tühikut; valitud või kombineeritud Unicode'i tekst ei purune; tühi või loetamatu sisestusväli ei põhjusta pimesi kustutust; tagasilükatud sisestustoiming jätab teksti muutmata. Kontrolli füüsilisel Pixel 8-l loomulikku puudutust, mitte vaid ADB ajastust.

### P0.4 Tuttav, aga oma kujundusega klaviatuur

**Võrdlus:** O1 näitas inglise QWERTY numbrivihjete, eraldi sümboli/emoji, Shift/Backspace ja kitsama tööriistaribaga. Gboardi juhend kirjeldab topelt-Shift'i ning klahvi hoidmist.[2] **Viipio nõue:** ET omaette õ/ä/ö/ü, ülemine numbririda, ?123, kirjavahemärgid, otsesed emoji- ja keeletoimingud, originaalne hele/tume vaade (V1). Gboardi pilt on mõõdureferents, mitte vara.

**Teostus:** üks klahviristkülikute mõõtmine joonistuse, puudutuse, juurdepääsetavuse ja viipedekoodri jaoks; pikalt hoidmise valik peab näitama ja sisestama õige sümboli. Kaardista UI-s ainult töötavad nupud. Säilita 48dp tööriistarea kõrgus ka kandidaatide/pakkumise/emoji puhul. Seadistatud topelt-Shift → Caps Lock, lausealguse suurtäht eraldi OFF/ON; klahvitähised ja TalkBack'i nimed järgivad sisestusolekut. Värvid, fondid ja ikoonid kujunda iseseisvalt.

**Valmis, kui:** ET/EN, sümbolid, pika hoidmise variandid, hele/tume ja kitsas/landscape säilitavad sama klahvi puute- ja dekoodrialad; sisestusväli jääb nähtavaks; ükski nupp ei ole dekoratiivne; süsteemi suurtähtede seadistus ei muuda kasutaja OFF-eelistust. PIN-, arvu-, telefoni- ja kuupäeva/kellaajaväli avavad **oma numbrivaate**, mitte QWERTY: PIN-is ainult `0–9`, Backspace ja kinnitustoiming; kümnend-/märgiväljal ainult lubatud märgid. Välja vahetamisel naaseb tähevaade; numbriväljas puuduvad viipe-, sõna-/emoji- ja diagnostikateed. Võrdle paigaldatud vaadet sama sünteetilise väljatüübiga Gboardiga, testides sisestuse päris readback'i; Gboardi pärisikoone ja varasid ära kopeeri. Kontrolli ka käega kiiret topelt-Shift'i, mida eraldi ADB-tapid ei tõesta.

### P0.5 Keel, isiklik sõnavara, privaatsus

**Võrdlus:** Gboardil on valitav keel/paigutus ning sõna lisamise tee.[3][6] **Viipio otsus:** kaks eraldi aktiivset keelt, keel märgistatud valitud olekuga. V0.6.0 tööpuu plaanib kinnitatavat inline `Add to Estonian/English words` pakkumist pärast Viipio enda lõpetatud sõna, mitte passiivset õppimist. Slängi `mhm/mhmh` saab lisada isiklikult; ühissõnastiku lisand eeldab lubatud allikat ja sihitud kontrolli.

**Teostus:** lisamise, kustutamise ja Undo toime nii käsitsi soovitustele kui ka mõlemale viipedekoodrile; kontrolli olemasolevaid vanu isiklike sõnade salvestusteid, otsekäivituse/varunduse erandeid ja ebaõnnestunud salvestuse tagasipööramist. Diagnostika jääb kolmes sõltumatus nõusolekus vaikimisi välja; sisestussündmused on ainult Viipio enda kontrollitud tavavälja toimingud, mitte rakenduse sisutekst. Tavaline väli võib siiski sisaldada isiklikku infot — ekspordi lihttekst vajab hoiatust ja kasutaja valitud sihtkohta.

**Valmis, kui:** lisatud ET sõna ei ilmne EN-is; vale salvestus, võõras sisestusväli ja parool/e-post/URL/number ei loo isiklikku sõna ega diagnostikakirjet; `Stop`/`Erase` püsivad ka taaskäivitusel; eksport on enne jagamist ülevaadatav. Avaliku APK õigustes pole vaikimisi Internetti ega mikrofoni; kontrolli seda *koostatud* APK-st.

## P1 — pärast toimivat põhisisestust

### P1.1 Võrguühenduseta emojiotsing ja väljundi kinnitamine

**Võrdlus:** Gboardi juhend lubab järjest mitut emojit valida [2]; O1 näitas kiirrida, hiljutisi emoji'sid, fondisuurusest sõltuvat kuva ja emoji-soovituse lüliteid. **Viipio nõue:** otsing eesti ja inglise sõnadega, kirjaviga taluv, ilma võrguühenduseta; GIF pole nõutav (V1).

**Teostus:** litsentsitud märksõnadest/Unicode'i nimetustest kinnine indeks; säilita EN esimese tulemuse järjestus, lisa ET alias ja hääliku/diakriitiku normaliseerimine, typo-fallback ainult sobivate täpse/prefiksi tulemuste puudumisel. Piira puu külastatavate sõlmede hulka, mitte üksnes tulemuste arvu. Otsinguväli IME sees ei tohi teist IME-d rekursiivselt avada; emoji-paneel mahub klaviatuuri tavakõrgusse. Lisa hiljutistesse ainult edukalt kinnitatud sisestus; pärast emoji valimist paneel jääb lahti kuni **ABC** või teadliku sulgemiseni.

**Valmis, kui:** õiged ET/EN täpsed ja ühe tähe veaga päringud annavad oodatud järjestatud tulemused ka lennurežiimis; kaks järjestikust valikut jõuavad sisestusvälja ja paneel jääb alles; ABC taastab tähed; ekraanile jääb nähtav sisestusväli, TalkBack kirjeldab emojit ja hiljutise eristused ei leki salajasse välja. Mõõda pakitud indeksi mälu ja otsingu viivitus.

### P1.2 Seadete selgus ja puudutustagasiside

**Võrdlus:** Gboardi teema, heli ja vibratsiooni valikud on juhendis [5]; O1 näitas reaalseid menüüsid number row, tap feedback, hold duration, text correction, glide ja emoji all. Viipios peavad vastavad **aktiivse teenuse** juhtseaded olema ühes ingliskeelses avalehe hierarhias, mitte päritud kuid mittetoimivad CleverKeysi nupud.

**Teostus:** eraldi jaotised `Typing`, `Gestures`, `Languages & personal words`, `Emoji`, `Privacy & diagnostics`, `Appearance`. Iga lüliti peab jõudma aktiivse IME-ni. Hoia topelt-tühiku punkti, klahvi hoidmise aja ja lausealguse suurtähe mõistlikud sõltumatud valikud; ärge muutke eelistusi lihtsalt testimiseks. Eelista süsteemi heledat/tumedat olekut; fondi skaala ning kontrast on ligipääsetavuskriteeriumid, mitte Gboardi visuaalvara koopia.

**Valmis, kui:** iga nähtav lüliti annab enne/pärast sisestuskatses vaadeldava erinevuse; diagnostikale navigeerimine ei lülita kogumist sisse; juhtseadmed on ligipääsetavad ka suure fondi ja TalkBack'iga; seadete Gear, IME süsteemiseade ja rakenduse ikoon avavad sama loogilise kodulehe.

### P1.3 Järgmise sõna pakkumise hinnang, mitte automaatne sisselülitus

O1 näitas `Next-word suggestions` ja `Smart Compose` lüliteid; Google piirab osa täiustatud soovitusfunktsioone keele/asukohaga.[3] Viipio esimeses katses võib teha eraldi kohaliku ET/EN järgmise sõna uurimuse *ainult omaniku soovitud juhul*: mõõda, kas piiratud kontekst parandab tegelikku valikut, ilma suvalist ümbritsevat rakenduseteksti salvestamata. Ära seo diagnostika nõusolekut vaikimisi ennustuse õppimisega; ära sega eri keeli. Valmisolek nõuab kasu/mälu/latentsuse tõendit ja eraldi OFF-võimalust.

## P2 — eraldi otsuse korral, mitte v0.6.0 sisse

- **Lõikelaud:** Gboard pakub kleepimist ja kinnitamist [4]; O1 näitas hiljutist teksti/pilte ja ekraanipiltide sätteid. Viipio võimalik esimene samm on üks **kasutaja vajutatud** praeguse lihttekst-klipi kleepimine; vaikimisi puudub ajalugu, taustakuulamine ja diagnostika. Ajalugu/pin vajab säilitus- ja tundliku klipi erandit ning eraldi privaatsusotsust.
- **Ühe käe režiim, suuruse kohandus, teemavalik:** O1 näitas `One-handed keyboard` ja `Theme`, [5] kirjeldab teemavalikut. Kavanda iseseisev originaalkujundus alles pärast põhiridade/puutepiirkondade eri suuruse katseid; ujuva klaviatuuri võimalust siin ei tõendatud.
- **GIF, kleepsud, Emoji Kitchen:** Gboardi juhend mainib GIF-e [2], O1 näitas vastavaid menüüsid. Omanik ei pea GIF-otsingut oluliseks; viipel, sümbolitel ja emojiotsingul on eesõigus. Võimalik uus võrgupääs/litsents vajab eraldi luba.
- **Hääl, tõlge, käekiri/puutepliiats, generatiivne „Fix it”:** Gboardi juhendid kirjeldavad neist osa [3][7][8], O1 näitas menüünuppe. Need ei ole Viipio võrguühenduseta ET-viibet vajava algfaasi eeldused. Mitte lubada nende ET toetust, privaatsust ega võrguühenduseta tööd ilma eraldi allika ja paigaldatud katsena; mikrofoni- või võrguluba ei tule „Gboardi sarnasuse” pärast kaasa.

## Väljalõiked arenduseks (mitte automaatsed versiooninumbrid)

1. **Sulge käimasolev v0.6.0:** isikliku sõna lisamine, nõusolekuga diagnostika ja etteantud viipekatse *oma* aktsepteerimiskriteeriumidega; ära nihuta selle APK-d P1/P2 lisamisega. Täiskomplekt, sõltumatu privaatsus- ja UI-ülevaatus, allkirjastatud ARM64 APK paigaldus/eksport/Stop/Erase ning tagasipööre eelmisele IME-le. Füüsilise Pixel 8 swipe'i kvaliteet märgi testijale mõõtmata.
2. **Järgmine iseseisev ET-kvaliteedi väljalõige (P0.1 + P0.2):** etteantud ja rivisõnade viiped, joondatud kandidaatide analüüs, külmkäivitus ja käsitsi parandus. Üks väike pööratav rankingumuutus ainult siis, kui vastasviiped ei halvene; sellele oma paigaldatav kandidaat.
3. **Sisestusvälja- ja parandustöövoog (P0.3):** tühiku kursoriloha ning sõna kustutuse/alternatiivide omandikontroll ühes terviklikus sisestusvälja katses; teine iseseisev APK, kui kasutatav.
4. **Emoji ja kujunduse viimistlus (P0.4 + P1.1):** võrguühenduseta ET/EN otsingu järjestus, korduv valik, hele/tume ja väikese ekraani kõrgus; eraldi testitav tulemus.
5. **Hiljem:** seadete puhastus ja klahvitagasiside (P1.2), siis vaid tõendatud kasuga muud mugavused. P2 vajab omaniku uut prioriteediotsust.

**Iga väljalõike ühine kvaliteedivärav:** testid → koostatud APK metaandmed/õigused/allkiri → paigaldatud tavaline ja Compose'i sisestusväli → parool/no-suggestions/keelevahetus → emulaatori algse oleku taastamine → füüsiline Pixel 8 sõrmekatse ning vajadusel vaadeldud Gboardi võrdlus samade mitteisiklike fraasidega. Ära nimeta testitavaks pelka lähtekoodi ega ära avalda poolikut kombinatsiooni, milles üks oluline lüliti või valik ei jõua aktiivsesse teenusesse.

## Sources

[1] https://support.google.com/gboard/answer/2811346?hl=en&co=GENIE.Platform%3DAndroid
[2] https://support.google.com/gboard/answer/2842292?hl=en&co=GENIE.Platform%3DAndroid
[3] https://support.google.com/gboard/answer/7068415?hl=en&co=GENIE.Platform%3DAndroid
[4] https://support.google.com/gboard/answer/10742542?hl=en
[5] https://support.google.com/gboard/answer/6102154?hl=en&co=GENIE.Platform%3DAndroid
[6] https://support.google.com/gboard/answer/7068494?hl=en&co=GENIE.Platform%3DAndroid
[7] https://support.google.com/gboard/answer/7421372?hl=en&co=GENIE.Platform%3DAndroid
[8] https://support.google.com/gboard/answer/9108773?hl=en&co=GENIE.Platform%3DAndroid
[9] https://support.google.com/gboard/answer/6380730?hl=en&co=GENIE.Platform%3DAndroid
