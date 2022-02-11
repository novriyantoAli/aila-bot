package ui

import Launcher.Companion.USER_DIR
import core.YoutubePlayer
import kotlinx.coroutines.*
import model.Account
import model.Configuration
import model.Task
import util.checkEmptyDirectory
import util.copyDirectory
import util.writeCredentials
import java.awt.Component
import java.awt.Container
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.swing.*
import kotlin.system.exitProcess


@DelicateCoroutinesApi
class MainWindow(t: String, private val os: String, private val configuration: Configuration): JFrame(), ActionListener {

    private val tasks = ArrayList<Task>()

    private var logsModel = DefaultListModel<String>()
    private val logJList = JList(logsModel)

    private var accountModel = DefaultListModel<Account>()
    private val accountJList = JList(accountModel)

    private  lateinit var checkLogin: JCheckBox
    private lateinit var menuDelete: JMenuItem
    private lateinit var menuExit: JMenuItem

    companion object {
        const val USER_NAME = "user.name"
    }

    init {
        title = t

        setSize(200, 450)

        layout = GridLayout(3,1)

        setLocationRelativeTo(null)

        val panel1 = createContainerButton()
        val panel2 = createContainerAccount()
        val panel3 = createContainerLog()

        add(panel1)
        add(panel2)
        add(panel3)

        isResizable = true
        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true
    }

    @DelicateCoroutinesApi
    private fun execute(youtube: YoutubePlayer) = GlobalScope.launch(Dispatchers.IO) {
        try {
            youtube.searchAndGoFirst(configuration.keyword)
            while(this.isActive){
                youtube.checkAd()
                if (youtube.hasAd)
                    youtube.skipAd()
                if (!youtube.hasAd && !youtube.hasMinimalQuality)
                    youtube.changeResolution()
                if (youtube.checkPlaylistEnd()) {
                    // quit driver
                    youtube.exit()
                    // delete profile
                    removeWithRegenerateTask(youtube.profileFolder)

                    break
//                    youtube.goToHomePage(false)
//                    youtube.searchAndGoFirst(configuration.keyword)
                }
                delay(5000)
            }
        } catch (e: NoSuchElementException) { e.printStackTrace() }
    }

    /**
     * INSTALL PROFILE ALGORITHM
     * 1. open browser, with local url
     */
    private fun installProfile() {
        for (i in configuration.configurationProfile) {
            if (os == i.os) {
                for (j in configuration.profile.detail) {
                    // check if empty
                    val profileName = configuration.profile.prefix + configuration.profile.split + j.name
                    if (checkEmptyDirectory((System.getProperty(USER_DIR)+"/"+profileName))) {
                        // delete in system
                        val path = i.pathPrefix + System.getProperty(USER_NAME) + i.pathPostfix + profileName
                        util.deleteDirectory(Paths.get(path).toFile())

                        copyDirectory(Paths.get((System.getProperty(USER_DIR)+"/"+i.profileDefault)), path)
                        // open browser
                        val arguments = arrayListOf<String>()
                        arguments.add(i.command.browser)
                        i.command.browserArguments.forEach { args ->
                            if (args.contains("profile-directory"))
                                arguments.add(args + profileName)
                        }
                        arguments.add("https://accounts.google.com")

                        writeCredentials(j.username, j.password)

                        val process = Runtime.getRuntime().exec(arguments.toTypedArray())
                        process.waitFor()

                        // copy again from system to local
                        copyDirectory(Paths.get(path),(System.getProperty(USER_DIR)+"/"+profileName))
                    }
                }

                break
            }
        }
    }

    /**
     * algorithm on login mode
     * 0. check if directory active in system
     * 1. delete directory in system
     * 2. copy directory from local to system
     * 3. launch chrome in random mode
     */
    private fun loginMode(){
        // delete directory in system
        for (i in configuration.configurationProfile) {
            if (System.getProperty(Launcher.OS_NAME).contains(i.os, ignoreCase = true)) {
                val profileLocation = i.pathPrefix + System.getProperty(USER_NAME) + i.pathPostfix
                for (j in configuration.profile.detail) {
                    var profileNowExists = false
                    for (k in accountModel.toArray()) {
                        val account = k as Account
                        if (account.profile.toString().contains(j.name, ignoreCase = true)) {
                            profileNowExists = true
                            break
                        }
                    }
                    if (!profileNowExists) {
                        val fullPath = profileLocation + configuration.profile.prefix + configuration.profile.split + j.name
                        val path = Paths.get(fullPath)
                        util.deleteDirectory(path.toFile())

                        // fourth step
                        copyDirectory((Paths.get((System.getProperty(USER_DIR)+ "/" + i.profileDefault))), fullPath)
                    }
                }

                // we will get profile without in accountModel adata
                val accountConvert = arrayListOf<Account>()
                for (item in configuration.profile.detail) {
                    val pn = configuration.profile.prefix + configuration.profile.split + item.name
                    accountConvert.add(Account(profile=pn,username=item.username,password=item.password))
                }

                for (item in accountModel.toArray()) {
                    val acc = item as Account
                    val res = accountConvert.stream()
                        .filter { ac -> ac.username.equals(acc.username) }
                        .collect(Collectors.toList())
                    if (res.size > 0)
                        accountConvert.remove(res[0])
                }

                if (accountConvert.size > 0) {
                    // now open random profile
                    val listRandomIndex = ArrayList<Int>()
                    var randomIndexProfile: Int
                    do {
                        randomIndexProfile = (0 until (accountConvert.size)).shuffled().first()
                        val valid: Boolean = if (listRandomIndex.size == 0) {
                            listRandomIndex.add(randomIndexProfile)
                            true
                        } else {
                            var localCheck = false
                            for (listing in listRandomIndex) {
                                if (listing == randomIndexProfile) {
                                    localCheck = true
                                    break
                                }
                            }
                            !localCheck
                        }
                    } while (!valid)

                    // open with random index
                    val youtube = YoutubePlayer(
                        profileFolder = accountConvert[randomIndexProfile].profile.toString(),
                        channelName = configuration.channelName,
                        playlistName = configuration.channelPlaylistTargetName,
                        playlistCode = configuration.channelPlaylistTargetCode,
                        profileLocation = profileLocation
                    )

                    val task = Task(
                        profileName = accountConvert[randomIndexProfile].profile.toString(),
                        job = execute(youtube),
                        driver = youtube
                    )

                    tasks.add(task)

                    accountModel.addElement(accountConvert[randomIndexProfile])
                } else {
                    JOptionPane.showMessageDialog(this@MainWindow, "sepertinya semua profile telah dipakai")
                }

                break
            }
        }
    }
    /**
     * algorithm not login
     * 1. if he not check login
     * 2. rise from profile one and check if account exists in account model
     * 3. if not exists, delete profile with name selected by looping from system and copy new profile in system using default_profile
     * 4. if exists, rise name profile from looping until you have empty profile
     * 5. open selenium using profile selected
     */
    private fun notLoginMode() {
        // check if exists
        var profileNumber = 1
        var profileName = "Profile"
        while (true) {
            var valLocalBol = true
            for (item in accountModel.toArray()) {
                val i = item as Account
                if (i.profile.toString().contains(("$profileName $profileNumber"), ignoreCase = true)){
                    valLocalBol = false
                    break
                }
            }
            if (valLocalBol)
                break

            profileNumber += 1
        }

        // delete directory from system
        var profileLocation: String? = null
        profileName = "$profileName $profileNumber"
        for (i in configuration.configurationProfile) {
            if (System.getProperty(Launcher.OS_NAME).contains(i.os, ignoreCase = true)) {
                profileLocation = i.pathPrefix + System.getProperty(USER_NAME) + i.pathPostfix

                val path = Paths.get((profileLocation + profileName))
                util.deleteDirectory(path.toFile())

                break
            }
        }

        // launch
        if (profileLocation != null) {
            val youtube = YoutubePlayer(
                profileFolder = profileName,
                channelName = configuration.channelName,
                playlistName = configuration.channelPlaylistTargetName,
                playlistCode = configuration.channelPlaylistTargetCode,
                profileLocation = profileLocation
            )

            val task = Task(
                profileName = profileName,
                job = execute(youtube),
                driver = youtube
            )

            tasks.add(task)

            accountModel.addElement(Account(profile = profileName))
        }
    }

    @DelicateCoroutinesApi
    private fun checkAccount() {
        if (checkLogin.isSelected) {
            for (i in configuration.profile.detail) {
                if (checkEmptyDirectory((System.getProperty(USER_DIR) + "/" + configuration.profile.prefix + configuration.profile.split + i.name))) {
                    JOptionPane.showMessageDialog(this@MainWindow, "lakukan instalasi profile terlebih dahulu")
                    return
                }
            }

            loginMode()
//
//            if (accountModel.size == 0) {
//                regenerateCache(configuration.profileList[0])
//
//                val pr = getPathProfileTo() ?: exitProcess(0)
//
//                val youtube = YoutubePlayer(
//                    profileFolder = configuration.profileList[0],
//                    channelName = configuration.channelName,
//                    playlistName = configuration.channelPlaylistTargetName,
//                    playlistCode = configuration.channelPlaylistTargetCode,
//                    profileLocation = ""
//                )
//
//                val task = Task(
//                    profileName = configuration.profileList[0],
//                    job = execute(youtube),
//                    driver = youtube
//                )
//
//                tasks.add(task)
//
//                accountModel.addElement(
//                    Account(
//                        username = configuration.configurationAccount[0].username,
//                        password = configuration.configurationAccount[0].password,
//                        profile = configuration.profileList[0]
//                    )
//                )
//                // get profile one
//                // execute browser
//            } else {
//                // first check account if not exists in account model
//                val accountConvert = arrayListOf<Account>()
//                for (item in configuration.configurationAccount) {
//                    accountConvert.add(Account(username = item.username, password = item.password))
//                }
//
//                for (item in accountModel.toArray()) {
//                    val i = item as Account
//                    val res = accountConvert.stream()
//                        .filter { ac -> ac.username.equals(i.username) }
//                        .collect(Collectors.toList())
//                    if (res.size > 0)
//                        accountConvert.remove(res[0])
//                }
//
//                if (accountConvert.size > 0) {
//                    // check if have empty profile
//                    val profileConvert = arrayListOf<Account>()
//                    for (item in configuration.profileList) {
//                        profileConvert.add(Account(profile = item))
//                    }
//
//                    for (item in accountModel.toArray()) {
//                        val i = item as Account
//                        val res = profileConvert.stream()
//                            .filter { ac -> ac.profile.equals(i.profile) }
//                            .collect(Collectors.toList())
//                        if (res.size > 0)
//                            profileConvert.remove(res[0])
//                    }
//
//                    if (profileConvert.size > 0) {
//                        regenerateCache(profileConvert[0].profile!!)
//
//                        accountConvert[0].profile = profileConvert[0].profile
//
//                        val pr = getPathProfileTo() ?: exitProcess(0)
//
//                        val youtube = YoutubePlayer(
//                            profileFolder = profileConvert[0].profile!!,
//                            channelName = configuration.channelName,
//                            playlistName = configuration.channelPlaylistTargetName,
//                            playlistCode = configuration.channelPlaylistTargetCode,
//                            profileLocation = ""
//                        )
//
//                        val task = Task(
//                            profileName = profileConvert[0].profile!!,
//                            job = execute(youtube),
//                            driver = youtube
//                        )
//
//                        tasks.add(task)
//                        accountModel.addElement(accountConvert[0])
//
//                    } else {
//                        JOptionPane.showMessageDialog(this@MainWindow, "Kehabisan profil bos")
//                        return
//                    }
//                } else {
//                    JOptionPane.showMessageDialog(this@MainWindow, "Akun Telah terisi selurunya")
//                    return
//                }
//            }
        } else {
            notLoginMode()
        }
    }

    private fun regenerateCache(profileSelected: String) {
        val pathProfileTo = getPathProfileTo() ?: exitProcess(0)
        val pth = Paths.get((pathProfileTo + profileSelected))
        if (Files.exists(pth)) {
            deleteDirectoryConfig(pth.toFile())
        }
        val fromPath = Paths.get(getPathProfileFrom())

        Files.walk(fromPath).forEach { source: Path -> copySourceToDest(fromPath, source, (pathProfileTo + profileSelected)) }
    }

    private fun getPathProfileTo(): String? {
        for (cp in configuration.configurationProfile) {
            if (System.getProperty(Launcher.OS_NAME).contains(cp.os, ignoreCase = true)) {
                return cp.pathPrefix + System.getProperty(USER_NAME) + cp.pathPostfix
            }
        }

        return null
    }

    private fun getPathProfileFrom(): String {
        return "${System.getProperty(USER_DIR)}/profile_${os}_default"
    }

    private fun copySourceToDest(fromPath: Path, source: Path, dstPath: String) {
        val destination = Paths.get(dstPath, source.toString().substring(fromPath.toString().length))
        try {
            Files.copy(source, destination)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun deleteDirectoryConfig(directoryToBeDeleted: File): Boolean {
        val allContents = directoryToBeDeleted.listFiles()
        if (allContents != null) {
            for (file in allContents)
                deleteDirectoryConfig(file)
        }
        return directoryToBeDeleted.delete()
    }

    @DelicateCoroutinesApi
    private fun createContainerButton(): Container {
        val myContainer = JPanel()

        myContainer.border = BorderFactory.createTitledBorder("Aksi")
        val boxLayout = BoxLayout(myContainer, BoxLayout.Y_AXIS)
        myContainer.layout = boxLayout

        checkLogin = JCheckBox("Akun Login")

        myContainer.add(checkLogin)

        val buttonAddNewChrome = JButton("Tambahkan Chrome")
        buttonAddNewChrome.alignmentX = Component.LEFT_ALIGNMENT
        buttonAddNewChrome.addActionListener { checkAccount() }

        val buttonInstall = JButton("Install Profil")
        buttonInstall.alignmentX = Component.LEFT_ALIGNMENT
        buttonInstall.addActionListener { installProfile() }

        myContainer.add(buttonAddNewChrome)
        myContainer.add(buttonInstall)

        return myContainer
    }

    private fun createContainerLog(): Container {
        val myContainer = JPanel()

        myContainer.border = BorderFactory.createTitledBorder("Log")
        val boxLayout = BoxLayout(myContainer, BoxLayout.Y_AXIS)
        myContainer.layout = boxLayout

        val scrollPane = JScrollPane()
        scrollPane.setViewportView(logJList)

        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS

        logJList.layoutOrientation = JList.VERTICAL
        logJList.setBounds(100,100,75,75)
        logJList.alignmentX = Component.RIGHT_ALIGNMENT

        myContainer.add(scrollPane)

        return myContainer
    }

    private fun createContainerAccount(): Container {
        val myContainer = JPanel()

        myContainer.border = BorderFactory.createTitledBorder("Akun")
        val boxLayout = BoxLayout(myContainer, BoxLayout.Y_AXIS)
        myContainer.layout = boxLayout

        val scrollPane = JScrollPane()
        scrollPane.setViewportView(accountJList)

        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS

        accountJList.layoutOrientation = JList.VERTICAL
        accountJList.setBounds(100,100,75,75)
        accountJList.alignmentX = Component.CENTER_ALIGNMENT

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

    private fun removeWithRegenerateTask(profileName: String) {
        var itemSelected: Task? = null
        for (item in tasks) {
            if (profileName.contains(item.profileName, ignoreCase = true)) {
                item.job.cancel()
                itemSelected = item
                break
            }
        }

        if (itemSelected != null) {
            tasks.remove(itemSelected)

            regenerateCache(itemSelected.profileName)

            accountModel.toArray().forEach {
                val account = it as Account
                if(account.profile!!.contains(itemSelected.profileName)) {
                    // create new task
                    for (conf in configuration.configurationProfile) {
                        if (System.getProperty(Launcher.OS_NAME).contains(conf.os, ignoreCase = true)) {
                            val youtube = YoutubePlayer(
                                playlistName = configuration.channelPlaylistTargetName,
                                playlistCode = configuration.channelPlaylistTargetCode,
                                profileFolder = profileName,
                                channelName = configuration.channelName,
                                profileLocation = (conf.pathPrefix + System.getProperty(USER_NAME) + conf.pathPostfix)
                            )

                            val task = Task(
                                profileName = account.profile!!,
                                job = execute(youtube),
                                driver = youtube
                            )

                            tasks.add(task)
                            break
                        }
                    }
                }
            }
        }
    }

    private fun exitTask(task: Task) = GlobalScope.launch (Dispatchers.IO){
        task.job.cancel()
        if (task.job.isCancelled) {
            task.driver.exit()
            tasks.remove(task)
            accountModel.remove(accountJList.selectedIndex)
        }
    }

    private fun deleteHistoryAndExit(task: Task) = GlobalScope.launch(Dispatchers.IO) {
        task.job.cancel()
        if (task.job.isCancelled) {
            task.driver.exit()

            delay(5000)

            regenerateCache(task.profileName)

            tasks.remove(task)
            accountModel.remove(accountJList.selectedIndex)
        }
    }

    override fun actionPerformed(p0: ActionEvent?) {
        if (p0?.source == menuExit) {
            for (task in tasks) {
                if (task.profileName.contains(accountJList.selectedValue.profile.toString(), ignoreCase = true)) {
                    exitTask(task)
                    break
                }
            }
        } else if (p0?.source == menuDelete) {
            for (task in tasks) {
                if (task.profileName.contains(accountJList.selectedValue.profile.toString(), ignoreCase = true)) {
                    deleteHistoryAndExit(task)
                    break
                }
            }
        }
    }
}