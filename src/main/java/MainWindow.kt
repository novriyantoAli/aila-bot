import com.github.cliftonlabs.json_simple.JsonArray
import com.github.cliftonlabs.json_simple.JsonObject
import com.github.cliftonlabs.json_simple.Jsoner
import kotlinx.coroutines.*
import java.awt.Component
import java.awt.Container
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.Reader
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.swing.*


@DelicateCoroutinesApi
class MainWindow(t: String): JFrame(), ActionListener {

    private val requests = ArrayList<Task>()

    private var logsModel = DefaultListModel<String>()
    private val logJList = JList(logsModel)

    private var accountModel = DefaultListModel<Account>()
    private val accountJList = JList(accountModel)

    private  lateinit var checkLogin: JCheckBox
    private lateinit var menuDelete: JMenuItem
    private lateinit var menuExit: JMenuItem

    init {
        title = t
        setSize(200, 450)
        layout = GridLayout(3,1)
        setLocationRelativeTo(null)

        val panel1 = createContainerButton("Aksi", Component.LEFT_ALIGNMENT)
        val panel2 = createContainerAccount("Akun", Component.CENTER_ALIGNMENT)
        val panel3 = createContainerLog("Log", Component.RIGHT_ALIGNMENT)

        add(panel1)
        add(panel2)
        add(panel3)

        isResizable = true
        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true
    }

    @DelicateCoroutinesApi
    private fun execute(youtube: YoutubePlayer, keyword: String, playlistName: String) = GlobalScope.launch(Dispatchers.IO) {
        try {

            youtube.clearCookie()

            if (youtube.username != null && youtube.password != null)
                youtube.goToLoginProcedure()

            youtube.goToHomePage(youtube.username != null && youtube.password != null)
            youtube.searchAndGoFirst(keyword)
            while(this.isActive){
                youtube.checkAd()
                if (youtube.hasAd)
                    youtube.skipAd()
                if (!youtube.hasAd && !youtube.hasMinimalQuality)
                    youtube.changeResolution()
                if (youtube.checkPlaylistEnd(playlistName) == null) {
                    youtube.clearHistory()
                    youtube.clearCookie()

                    delay(7000)

                    youtube.goToHomePage(false)
                    youtube.searchAndGoFirst(keyword)
                }
                delay(5000)
            }
        } catch (e: NoSuchElementException) { e.printStackTrace() }
    }

    @DelicateCoroutinesApi
    private fun checkAccount() {
        val path = (Paths.get("").toAbsolutePath().toString() + "/config.json")
        val selectedFile = File(path)
        val reader: Reader = selectedFile.bufferedReader()
        // create parser
        val parser = Jsoner.deserialize(reader) as JsonObject

        val ky = parser["kata_kunci"] as String
        val pln = parser["nama_playlist"] as String
        val ch = parser["kode_channel"] as String
        val kpl = parser["kode_playlist"] as String
        val hpl = parser["kode_halaman_playlist"] as String
        val tp = parser["tempat_profile"] as String

        val profiles = parser["profile"] as JsonArray
        val account = parser["akun"] as JsonArray

        if (checkLogin.isSelected) {
            if (account.size == 0) {
                JOptionPane.showMessageDialog(this@MainWindow, "Periksa konfigurasi, anda belum menautkan akun")
                return
            }

            if (profiles.size == 0) {
                JOptionPane.showMessageDialog(this@MainWindow, "Periksa konfigurasi, anda belum menautkan profile")
                return
            }

            if (accountModel.size == 0) {
                val acc = account[0] as JsonObject

                val youtube = YoutubePlayer(
                    username = acc["pengguna"] as String,
                    password = acc["sandi"] as String,
                    playlistHome= hpl,
                    channel = ch,
                    playlistWatch = kpl,
                    profile = tp + profiles[0] as String
                )

                val task = Task(
                    profileName = profiles[0] as String,
                    job = execute(
                        youtube = youtube,
                        playlistName = pln,
                        keyword = ky,
                    ),
                    driver = youtube
                )
                requests.add(task)

                accountModel.addElement(
                    Account(
                        username = acc["pengguna"] as String,
                        password = acc["sandi"] as String,
                        profile = profiles[0] as String
                    )
                )
                // get profile one
                // execute browser
            } else {
                // first check account if not exists in account model
                val accountConvert = arrayListOf<Account>()
                for (item in account) {
                    val temp = (item as JsonObject)
                    accountConvert.add(
                        Account(username = (temp["pengguna"] as String), password = (temp["sandi"] as String))
                    )
                }

                for (item in accountModel.toArray()) {
                    val i = item as Account
                    val res = accountConvert.stream()
                        .filter { ac -> ac.username.equals(i.username) }
                        .collect(Collectors.toList())
                    if (res.size > 0)
                        accountConvert.remove(res[0])
                }

                if (accountConvert.size > 0) {
                    // check if have empty profile
                    val profileConvert = arrayListOf<Account>()
                    for (item in profiles) {
                        profileConvert.add(Account(profile = (item as String)))
                    }

                    for (item in accountModel.toArray()) {
                        val i = item as Account
                        val res = profileConvert.stream()
                            .filter { ac -> ac.profile.equals(i.profile) }
                            .collect(Collectors.toList())
                        if (res.size > 0)
                            profileConvert.remove(res[0])
                    }

                    if (profileConvert.size > 0) {
                        accountConvert[0].profile = profileConvert[0].profile
                        val youtube = YoutubePlayer(
                            username = accountConvert[0].username,
                            password = accountConvert[0].password,
                            playlistHome= hpl,
                            channel = ch,
                            playlistWatch = kpl,
                            profile = tp + profileConvert[0].profile!!
                        )

                        val task = Task(
                            profileName = profiles[0] as String,
                            job = execute(
                                youtube = youtube,
                                playlistName = pln,
                                keyword = ky,
                            ),
                            driver = youtube
                        )
                        requests.add(task)
                        accountModel.addElement(accountConvert[0])

                    } else {
                        JOptionPane.showMessageDialog(this@MainWindow, "Kehabisan profil bos")
                        return
                    }
                    // get
                } else {
                    JOptionPane.showMessageDialog(this@MainWindow, "Akun Telah terisi selurunya")
                    return
                }
            }
        } else {
            // search profile empty
            val profileConvert = arrayListOf<Account>()
            for (item in profiles) {
                profileConvert.add(Account(profile = (item as String)))
            }

            for (item in accountModel.toArray()) {
                val i = item as Account
                val res = profileConvert.stream()
                    .filter { ac -> ac.profile.equals(i.profile, ignoreCase = true) }
                    .collect(Collectors.toList())
                if (res.size > 0)
                    profileConvert.remove(res[0])
            }

            if (profileConvert.size > 0) {
                val youtube = YoutubePlayer(
                    username = null,
                    password = null,
                    playlistHome= hpl,
                    channel = ch,
                    playlistWatch = kpl,
                    profile = tp + profileConvert[0].profile!!
                )

                val task = Task(
                    profileName = profileConvert[0].profile!!,
                    job = execute(
                        youtube = youtube,
                        playlistName = pln,
                        keyword = ky,
                    ),
                    driver = youtube
                )

                requests.add(task)

                accountModel.addElement(profileConvert[0])
            } else {
                JOptionPane.showMessageDialog(this@MainWindow, "Kehabisan profil bos")
                return
            }
        }
    }

    @DelicateCoroutinesApi
    private fun createContainerButton(title: String, alignment: Float): Container {
        val myContainer = JPanel()

        myContainer.border = BorderFactory.createTitledBorder(title)
        val boxLayout = BoxLayout(myContainer, BoxLayout.Y_AXIS)
        myContainer.layout = boxLayout

        checkLogin = JCheckBox("Akun Login")

        myContainer.add(checkLogin)

        val buttonAddNewChrome = JButton("Tambahkan Chrome")
        buttonAddNewChrome.alignmentX = alignment
        buttonAddNewChrome.addActionListener {
            checkAccount()
        }
        myContainer.add(buttonAddNewChrome)

        return myContainer
    }

    private fun createContainerLog(title: String, alignment: Float): Container {
        val myContainer = JPanel()

        myContainer.border = BorderFactory.createTitledBorder(title)
        val boxLayout = BoxLayout(myContainer, BoxLayout.Y_AXIS)
        myContainer.layout = boxLayout

        val scrollPane = JScrollPane()
        scrollPane.setViewportView(logJList)

        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS

        logJList.layoutOrientation = JList.VERTICAL
        logJList.setBounds(100,100,75,75)
        logJList.alignmentX = alignment

        myContainer.add(scrollPane)

        return myContainer
    }

    private fun createContainerAccount(title: String, alignment: Float): Container {
        val myContainer = JPanel()

        myContainer.border = BorderFactory.createTitledBorder(title)
        val boxLayout = BoxLayout(myContainer, BoxLayout.Y_AXIS)
        myContainer.layout = boxLayout

        val scrollPane = JScrollPane()
        scrollPane.setViewportView(accountJList)

        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS

        accountJList.layoutOrientation = JList.VERTICAL
        accountJList.setBounds(100,100,75,75)
        accountJList.alignmentX = alignment

        val popupMenu = JPopupMenu()
        popupMenu.add(JMenuItem("Hapus dan bersihkan")).also { menuDelete = it }
        popupMenu.add(JMenuItem("Keluar")).also { menuExit = it }

        menuDelete.addActionListener(this)

        accountJList.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e) && !accountJList.isSelectionEmpty && accountJList.locationToIndex(e.point) == accountJList.selectedIndex) {
                    popupMenu.show(accountJList, e.x, e.y)
                }
            }
        })

        myContainer.add(scrollPane)

        return myContainer
    }

    private fun exitTask(task: Task) = GlobalScope.launch (Dispatchers.IO){
        task.job.cancel()
        if (task.job.isCancelled) {
            task.driver.exit()
            requests.remove(task)
            accountModel.remove(accountJList.selectedIndex)
        }
    }

    private fun deleteHistoryAndExit(task: Task) = GlobalScope.launch(Dispatchers.IO) {
        task.job.cancel()
        if (task.job.isCancelled) {
            task.driver.clearCookie()
            task.driver.exit()

            requests.remove(task)
            accountModel.remove(accountJList.selectedIndex)
        }
    }

    override fun actionPerformed(p0: ActionEvent?) {
        if (p0?.source == menuExit) {
            for (task in requests) {
                if (task.profileName.contains(accountJList.selectedValue.profile.toString(), ignoreCase = true)) {
                    exitTask(task)
                    break
                }
            }
        } else if (p0?.source == menuDelete) {
            for (task in requests) {
                if (task.profileName.contains(accountJList.selectedValue.profile.toString(), ignoreCase = true)) {
                    deleteHistoryAndExit(task)
                    break
                }
            }
        }
    }
}