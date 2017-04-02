package net.squanchy.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.design.widget.Snackbar;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseUser;

import net.squanchy.BuildConfig;
import net.squanchy.R;
import net.squanchy.navigation.Navigator;
import net.squanchy.onboarding.OnboardingPage;
import net.squanchy.proximity.preconditions.ProximityOptInPersister;
import net.squanchy.service.proximity.injection.ProximityService;
import net.squanchy.signin.SignInService;
import net.squanchy.support.lang.Optional;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class SettingsFragment extends PreferenceFragment {

    public static final int REQUEST_TURN_LOCATION_ON = 6429;

    private SignInService signInService;
    private Navigator navigator;
    private ProximityService proximityService;
    private ProximityOptInPersister proximityOptInPersister;

    private PreferenceCategory accountCategory;
    private Preference accountEmailPreference;
    private Preference accountSignInSignOutPreference;
    private SwitchPreference locationPreferences;

    private Disposable subscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_preferences);
        getPreferenceScreen().setOrderingAsAdded(false);

        displayBuildVersion();

        if (!BuildConfig.DEBUG) {
            removeDebugCategory();
        }

        SettingsComponent component = SettingsInjector.obtain(getActivity());
        signInService = component.signInService();
        navigator = component.navigator();
        proximityService = component.proximityService();
        proximityOptInPersister = component.proximityOptInPersister();

        accountCategory = (PreferenceCategory) findPreference(getString(R.string.account_category_key));
        accountEmailPreference = findPreference(getString(R.string.account_email_preference_key));
        accountCategory.removePreference(accountEmailPreference);
        accountSignInSignOutPreference = findPreference(getString(R.string.account_signin_signout_preference_key));
        locationPreferences = (SwitchPreference) findPreference(getString(R.string.location_preference_key));
        locationPreferences.setOnPreferenceChangeListener((preference, isEnabling) ->
                handleLocationPreferenceChange((boolean) isEnabling)
        );

        Preference aboutPreference = findPreference(getString(R.string.about_preference_key));
        aboutPreference.setOnPreferenceClickListener(preference -> {
            navigator.toAboutSquanchy();
            return true;
        });
    }

    private void displayBuildVersion() {
        String buildVersionKey = getString(R.string.build_version_preference_key);
        Preference buildVersionPreference = findPreference(buildVersionKey);
        String buildVersion = String.format(getString(R.string.version_x), BuildConfig.VERSION_NAME);
        buildVersionPreference.setTitle(buildVersion);
    }

    private void removeDebugCategory() {
        String debugCategoryKey = getString(R.string.debug_category_preference_key);
        Preference debugCategory = findPreference(debugCategoryKey);
        getPreferenceScreen().removePreference(debugCategory);
    }

    @Override
    public void onStart() {
        super.onStart();

        locationPreferences.setChecked(proximityOptInPersister.userOptedIn());

        hideDividers();

        subscription = signInService.currentUser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUserChanged);
    }

    private void hideDividers() {
        ListView list = (ListView) getView().findViewById(android.R.id.list);
        list.setDivider(null);
        list.setDividerHeight(0);
    }

    private void onUserChanged(Optional<FirebaseUser> user) {
        if (user.isPresent() && !user.get().isAnonymous()) {
            onSignedInWith(user.get());
        } else {
            onSignedOut();
        }
    }

    private void onSignedInWith(FirebaseUser firebaseUser) {
        accountCategory.addPreference(accountEmailPreference);
        accountEmailPreference.setTitle(firebaseUser.getEmail());

        accountSignInSignOutPreference.setTitle(R.string.sign_out_title);
        accountSignInSignOutPreference.setOnPreferenceClickListener(
                preference -> {
                    signInService.signOut()
                            .subscribe(() ->
                                    Snackbar.make(getView(), R.string.settings_message_signed_out, Snackbar.LENGTH_SHORT).show()
                            );
                    return true;
                }
        );
    }

    private void onSignedOut() {
        accountCategory.removePreference(accountEmailPreference);

        accountSignInSignOutPreference.setTitle(R.string.sign_in_title);
        accountSignInSignOutPreference.setOnPreferenceClickListener(
                preference -> {
                    navigator.toSignIn();
                    return true;
                }
        );
    }

    private boolean handleLocationPreferenceChange(boolean isEnabling) {
        if (isEnabling) {
            navigator.toOnboardingForResult(OnboardingPage.LOCATION, REQUEST_TURN_LOCATION_ON);
            return true;
        } else {
            proximityService.stopRadar();
            proximityOptInPersister.storeUserOptedOut();
            return true;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        subscription.dispose();
    }
}
