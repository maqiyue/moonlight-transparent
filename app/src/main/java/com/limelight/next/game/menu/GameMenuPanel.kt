package com.moonlight.next.game.menu

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Games
import androidx.compose.material.icons.rounded.KeyboardCommandKey
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.Task
import androidx.compose.material.icons.rounded.TouchApp
import com.limelight.Game
import com.limelight.R
import com.limelight.binding.input.GameInputDevice
import com.limelight.binding.input.KeyboardTranslator
import com.limelight.nvstream.NvConnection
import com.limelight.nvstream.input.KeyboardPacket
import com.limelight.preferences.PreferenceConfiguration
import com.limelight.next.game.menu.options.CancelMenuOption
import com.limelight.next.game.menu.options.DisconnectMenuOption
import com.limelight.next.game.menu.options.FloatingKeyboardMenuOption
import com.limelight.next.game.menu.options.KeyboardMenuOption
import com.limelight.next.game.menu.options.MenuOption
import com.limelight.next.game.menu.options.MenuPanelOption
import com.limelight.next.game.menu.options.PanZoomModeMenuOption
import com.limelight.next.game.menu.options.PerformanceOverlayMenuOption
import com.limelight.next.game.menu.options.QuitSessionMenuOption
import com.limelight.next.game.menu.options.SendKeysMenuOption
import com.limelight.next.game.menu.options.SpecialButtonMenuOption
import com.limelight.next.game.menu.options.ThreeFingerKeyboardMenuOption
import com.limelight.next.game.menu.options.VirtualKeyboardMenuOption
import com.limelight.next.game.menu.showMenuPanelDialog
import org.json.JSONObject

class GameMenuPanel(
    private val game: Game,
    private val device: GameInputDevice?
) {

    init {
        showMenu()
    }

    private fun getString(id: Int): String {
        return game.resources.getString(id)
    }

    private fun runWithGameFocus(runnable: Runnable?) {
        // Ensure that the Game activity is still active (not finished)
        if (game.isFinishing || runnable == null) {
            return
        }
        // Check if the game window has focus again, if not try again after delay
        if (!game.hasWindowFocus()) {
            Handler().postDelayed({ runWithGameFocus(runnable) }, TEST_GAME_FOCUS_DELAY)
            return
        }
        // Game Activity has focus, run runnable
        runnable.run()
    }

    private fun showMenuDialog(title: String, options: List<MenuPanelOption>) {
        showMenuPanelDialog(game, title, options, ::runWithGameFocus)
    }

    public fun showSpecialKeysMenu() {
        val options: MutableList<MenuPanelOption> = ArrayList()

        SpecialButtonMenuOption.init()

        if (!PreferenceConfiguration.readPreferences(game).enableClearDefaultSpecial) {
            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_esc)
            ) { game.sendKeys(shortArrayOf(KeyboardTranslator.VK_ESCAPE.toShort())) })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_f11)
            ) { game.sendKeys(shortArrayOf(KeyboardTranslator.VK_F11.toShort())) })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_alt_f4)
            ) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LMENU.toShort(),
                        KeyboardTranslator.VK_F4.toShort()
                    )
                )
            })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_alt_enter)
            ) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LMENU.toShort(),
                        KeyboardTranslator.VK_RETURN.toShort()
                    )
                )
            })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_ctrl_v)
            ) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LCONTROL.toShort(),
                        KeyboardTranslator.VK_V.toShort()
                    )
                )
            })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_win)
            ) { game.sendKeys(shortArrayOf(KeyboardTranslator.VK_LWIN.toShort())) })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_win_d)
            ) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LWIN.toShort(),
                        KeyboardTranslator.VK_D.toShort()
                    )
                )
            })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_win_g)
            ) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LWIN.toShort(),
                        KeyboardTranslator.VK_G.toShort()
                    )
                )
            })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_ctrl_alt_tab)
            ) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LCONTROL.toShort(),
                        KeyboardTranslator.VK_LMENU.toShort(),
                        KeyboardTranslator.VK_TAB.toShort()
                    )
                )
            })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_shift_tab)
            ) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LSHIFT.toShort(),
                        KeyboardTranslator.VK_TAB.toShort()
                    )
                )
            })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_win_shift_left)
            ) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LWIN.toShort(),
                        KeyboardTranslator.VK_LSHIFT.toShort(),
                        KeyboardTranslator.VK_LEFT.toShort()
                    )
                )
            })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_ctrl_alt_shift_q)
            ) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LCONTROL.toShort(),
                        KeyboardTranslator.VK_LMENU.toShort(),
                        KeyboardTranslator.VK_LSHIFT.toShort(),
                        KeyboardTranslator.VK_Q.toShort()
                    )
                )
            })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_ctrl_alt_shift_f1)
            ) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LCONTROL.toShort(),
                        KeyboardTranslator.VK_LMENU.toShort(),
                        KeyboardTranslator.VK_LSHIFT.toShort(),
                        KeyboardTranslator.VK_F1.toShort()
                    )
                )
            })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_ctrl_alt_shift_f12)
            ) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LCONTROL.toShort(),
                        KeyboardTranslator.VK_LMENU.toShort(),
                        KeyboardTranslator.VK_LSHIFT.toShort(),
                        KeyboardTranslator.VK_F12.toShort()
                    )
                )
            })

            options.add(SpecialButtonMenuOption(
                getString(R.string.game_menu_send_keys_alt_b)
            ) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LWIN.toShort(),
                        KeyboardTranslator.VK_LMENU.toShort(),
                        KeyboardTranslator.VK_B.toShort()
                    )
                )
            })
            options.add(SpecialButtonMenuOption(getString(R.string.game_menu_send_keys_win_x_u_s)) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LWIN.toShort(),
                        KeyboardTranslator.VK_X.toShort()
                    )
                )
                Handler().postDelayed((Runnable {
                    game.sendKeys(
                        shortArrayOf(
                            KeyboardTranslator.VK_U.toShort(),
                            KeyboardTranslator.VK_S.toShort()
                        )
                    )
                }), 200)
            })
            options.add(SpecialButtonMenuOption(getString(R.string.game_menu_send_keys_win_x_u_u)) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LWIN.toShort(),
                        KeyboardTranslator.VK_X.toShort()
                    )
                )
                Handler().postDelayed((Runnable {
                    game.sendKeys(
                        shortArrayOf(
                            KeyboardTranslator.VK_U.toShort(),
                            KeyboardTranslator.VK_U.toShort()
                        )
                    )
                }), 200)
            })
            options.add(SpecialButtonMenuOption(getString(R.string.game_menu_send_keys_win_x_u_r)) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LWIN.toShort(),
                        KeyboardTranslator.VK_X.toShort()
                    )
                )
                Handler().postDelayed((Runnable {
                    game.sendKeys(
                        shortArrayOf(
                            KeyboardTranslator.VK_U.toShort(),
                            KeyboardTranslator.VK_R.toShort()
                        )
                    )
                }), 200)
            })
            options.add(SpecialButtonMenuOption(getString(R.string.game_menu_send_keys_win_x_u_i)) {
                game.sendKeys(
                    shortArrayOf(
                        KeyboardTranslator.VK_LWIN.toShort(),
                        KeyboardTranslator.VK_X.toShort()
                    )
                )
                Handler().postDelayed((Runnable {
                    game.sendKeys(
                        shortArrayOf(
                            KeyboardTranslator.VK_U.toShort(),
                            KeyboardTranslator.VK_I.toShort()
                        )
                    )
                }), 200)
            })
        }

        //自定义导入的指令
        val preferences = game.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE)
        val value = preferences.getString(KEY_NAME, "")

        if (!TextUtils.isEmpty(value)) {
            try {
                val `object` = JSONObject(value)
                val array = `object`.optJSONArray("data")
                if (array != null && array.length() > 0) {
                    for (i in 0 until array.length()) {
                        val object1 = array.getJSONObject(i)
                        val name = object1.optString("name")
                        val array1 = object1.getJSONArray("data")
                        val datas = ShortArray(array1.length())
                        for (j in 0 until array1.length()) {
                            val code = array1.getString(j)
                            datas[j] = code.substring(2).toInt(16).toShort()
                        }
                        val option = MenuOption(name) { game.sendKeys(datas) }
                        options.add(option)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(game, getString(R.string.wrong_import_format), Toast.LENGTH_SHORT)
                    .show()
            }
        }
        options.add(CancelMenuOption(game))

        showMenuDialog(getString(R.string.game_menu_send_keys), options)
    }

    private fun showAdvancedMenu() {
        val options = mutableListOf<MenuOption>()

        options.add(MenuOption(
            getString(R.string.game_menu_toggle_keyboard_model),
            true,
            Icons.Rounded.KeyboardCommandKey
        ) { game.showHideKeyboardController() })

        options.add(MenuOption(
            getString(R.string.game_menu_toggle_virtual_model), true, Icons.Rounded.Games
        ) { game.showHideVirtualController() })




        options.add(MenuOption(
            getString(R.string.game_menu_switch_touch_sensitivity_model),
            true,
            Icons.Rounded.TouchApp
        ) { game.switchTouchSensitivity() })

        if (device != null) {
            var gameMenuOptions = device.getGameMenuOptions()
           //TODO : 转换格式
            device.gameMenuOptions.forEach {

            }
        }

        options.add(CancelMenuOption(game))

        showMenuDialog(getString(R.string.game_menu_advanced), options)
    }

    private fun showMenu() {
        val options: MutableList<MenuPanelOption> = ArrayList()

        options.add(QuitSessionMenuOption(game))
        options.add(DisconnectMenuOption(game))




        options.add(MenuOption(
            getString(R.string.game_menu_rotate_screen), true, Icons.Rounded.Autorenew,
        ) { game.rotateScreen() })

        options.add(FloatingKeyboardMenuOption(game))
        options.add(ThreeFingerKeyboardMenuOption(game))
        options.add(PanZoomModeMenuOption(game))


        options.add(PerformanceOverlayMenuOption(game))


        options.add(KeyboardMenuOption(game))

        options.add(VirtualKeyboardMenuOption(game))
        if (game.presentation == null) {
            options.add(MenuOption(
                getString(R.string.game_menu_select_mouse_mode), true, Icons.Rounded.Mouse
            ) {
                game.selectMouseMode()
            })
        }
        options.add(SendKeysMenuOption(game,this))
        options.add(MenuOption(
            getString(R.string.game_menu_task_manager), true, Icons.Rounded.Task
        ) {
            game.sendKeys(
                shortArrayOf(
                    KeyboardTranslator.VK_LCONTROL.toShort(),
                    KeyboardTranslator.VK_LSHIFT.toShort(),
                    KeyboardTranslator.VK_ESCAPE.toShort()
                )
            )
        })

        options.add(MenuOption(
            getString(R.string.game_menu_advanced), true, Icons.Rounded.Apps,
        ) { this.showAdvancedMenu() })

        options.add(CancelMenuOption(game))

        showMenuDialog(getString(R.string.quick_menu_title), options)
    }

    companion object {
        private const val TEST_GAME_FOCUS_DELAY: Long = 10
     

        const val PREF_NAME: String = "specialPrefs" // SharedPreferences的名称

        const val KEY_NAME: String = "special_key" // 要保存的键名称

       
    }
}