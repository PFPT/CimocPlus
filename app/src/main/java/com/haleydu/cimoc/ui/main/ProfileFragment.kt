package com.haleydu.cimoc.ui.main
import com.haleydu.cimoc.ui.common.BaseFragment
import android.content.Intent
import android.view.View
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatDelegate
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.ThemeResponsive
import com.haleydu.cimoc.databinding.FragmentProfileBinding
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.ui.settings.AboutActivity
import com.haleydu.cimoc.ui.settings.BackupActivity
import com.haleydu.cimoc.ui.main.MainActivity
import com.haleydu.cimoc.ui.settings.SettingsActivity
import com.haleydu.cimoc.ui.explore.SourceActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : BaseFragment(), ThemeResponsive {

    private var binding: FragmentProfileBinding? = null

    override fun getLayoutRes(): Int = R.layout.fragment_profile

    override fun bindViews(view: View) {
        super.bindViews(view)
        binding = FragmentProfileBinding.bind(view)
    }

    override fun initView() {
        val binding = binding ?: return
        binding.profileDownload.setOnClickListener {
            (activity as? MainActivity)?.openLibraryDownload()
        }
        binding.profileSource.setOnClickListener {
            startActivity(SourceActivity.createIntent(requireActivity()))
        }
        binding.profileBackup.setOnClickListener {
            startActivity(Intent(requireActivity(), BackupActivity::class.java))
        }
        binding.profileSettings.setOnClickListener {
            (activity as? MainActivity)?.openSettings()
        }
        binding.profileAbout.setOnClickListener {
            startActivity(Intent(requireActivity(), AboutActivity::class.java))
        }
        val night = mPreference.getBoolean(PreferenceManager.PREF_NIGHT, false)
        binding.profileNightLabel.setText(if (night) R.string.drawer_night else R.string.drawer_light)
        binding.profileNightSwitch.isChecked = night
        binding.profileNightSwitch.setOnCheckedChangeListener { _, isChecked ->
            mPreference.putBoolean(PreferenceManager.PREF_NIGHT, isChecked)
            binding.profileNightLabel.setText(if (isChecked) R.string.drawer_night else R.string.drawer_light)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            AppEventBus.post(AppEvent(AppEvent.EVENT_SWITCH_NIGHT, isChecked))
        }
    }

    override fun onThemeChange(@ColorRes primary: Int, @ColorRes accent: Int) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
