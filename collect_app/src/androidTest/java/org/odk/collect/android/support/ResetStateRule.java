package org.odk.collect.android.support;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.odk.collect.android.TestSettingsProvider;
import org.odk.collect.android.application.Collect;

import org.odk.collect.android.database.FormsDatabaseHelper;
import org.odk.collect.android.injection.config.AppDependencyModule;
import org.odk.collect.android.preferences.source.SettingsProvider;
import org.odk.collect.android.provider.InstanceProvider;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.utilities.MultiClickGuard;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.io.FileUtils.deleteDirectory;

public class ResetStateRule implements TestRule {

    private final AppDependencyModule appDependencyModule;
    private final SettingsProvider settingsProvider = TestSettingsProvider.getSettingsProvider();

    public ResetStateRule() {
        this(null);
    }

    public ResetStateRule(AppDependencyModule appDependencyModule) {
        this.appDependencyModule = appDependencyModule;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new ResetStateStatement(base);
    }

    private class ResetStateStatement extends Statement {

        private final Statement base;

        ResetStateStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

            resetDagger();
            clearPrefs();
            clearDisk();
            setTestState();

            // Reinitialize any application state with new deps/state
            ((Collect) context.getApplicationContext()).getComponent().applicationInitializer().initialize();

            base.evaluate();
        }
    }

    private void setTestState() {
        MultiClickGuard.test = true;
    }

    private void clearDisk() {
        try {
            StoragePathProvider storagePathProvider = new StoragePathProvider();
            deleteDirectory(new File(storagePathProvider.getOdkRootDirPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        FormsDatabaseHelper.recreateDatabaseHelper();
        InstanceProvider.recreateDatabaseHelper();
    }

    private void resetDagger() {
        if (appDependencyModule != null) {
            CollectHelpers.overrideAppDependencyModule(appDependencyModule);
        } else {
            CollectHelpers.overrideAppDependencyModule(new AppDependencyModule());
        }
    }

    private void clearPrefs() {
        settingsProvider.getGeneralSettings().clear();
        settingsProvider.getGeneralSettings().setDefaultForAllSettingsWithoutValues();
        settingsProvider.getAdminSettings().clear();
        settingsProvider.getAdminSettings().setDefaultForAllSettingsWithoutValues();
        settingsProvider.getMetaSettings().clear();
    }
}
