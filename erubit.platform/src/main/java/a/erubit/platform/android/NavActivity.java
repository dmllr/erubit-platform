package a.erubit.platform.android;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;

import a.erubit.platform.interaction.AnalyticsManager;
import a.erubit.platform.R;
import a.erubit.platform.course.Course;
import a.erubit.platform.course.CourseManager;
import a.erubit.platform.course.Lesson;
import t.TinyDB;
import u.C;
import u.U;

public class NavActivity
        extends AppCompatActivity
        implements
            CoursesFragment.OnCourseInteractionListener,
            LessonsFragment.OnLessonInteractionListener,
            TrainingFragment.OnTrainingInteractionListener,
            NavigationView.OnNavigationItemSelectedListener,
            SharedPreferences.OnSharedPreferenceChangeListener {

    private View mViewPermissionsWarning;
    private View mFab;

    private void setContentView() {
        setContentView(R.layout.activity_navigation);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView();

        if (savedInstanceState == null) {
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction()
                    .replace(R.id.fragment, new CoursesFragment())
                    .commit();
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                // show back button
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            } else {
                //show hamburger
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                toggle.syncState();
                toolbar.setNavigationOnClickListener(v -> drawer.openDrawer(GravityCompat.START));
            }
        });

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mFab = findViewById(R.id.fab);
        mViewPermissionsWarning = findViewById(R.id.permissionsWarning);
        mViewPermissionsWarning.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                askForPermission();
        });

        final SwitchCompat switchUnlock = ((SwitchCompat) findViewById(R.id.switchEnableOnUnlock));
        switchUnlock.setChecked(new TinyDB(getApplicationContext()).getBoolean(C.SP_ENABLED_ON_UNLOCK, true));
        switchUnlock.setOnCheckedChangeListener((buttonView, isChecked) -> new TinyDB(getApplicationContext()).putBoolean(C.SP_ENABLED_ON_UNLOCK, isChecked));

        mFab.setOnClickListener(view -> trainingButtonTapped());
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkDrawOverlayPermission();
        } else
            permissionGranted();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new TinyDB(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        new TinyDB(this).registerOnSharedPreferenceChangeListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void askForPermission() {
        new TinyDB(this).putBoolean(C.SP_PERMISSION_REQUESTED, true);

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
                    break;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.grant_permission_dialog)
                .setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private final static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 5432;

    private void permissionGranted() {
        mViewPermissionsWarning.setVisibility(View.GONE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkDrawOverlayPermission() {
        if (Settings.canDrawOverlays(this)){
            permissionGranted();
        } else {
            boolean permissionRequested = new TinyDB(this).getBoolean(C.SP_PERMISSION_REQUESTED, false);
            if (!permissionRequested)
                askForPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            // check once again if we have permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // continue here - permission was granted
                permissionGranted();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_share) {
            String message = CourseManager.i().getSharingText(this);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, message);

            startActivity(Intent.createChooser(share, getString(R.string.share_my_progress)));
        }
        if (id == R.id.nav_settings) {
            putFragment(new PreferencesFragment());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            popFragment();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void trainingButtonTapped() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment topFragment = fm.findFragmentById(R.id.fragment);

        Fragment trainingFragment = new CourseTrainingFragment();

        if (topFragment instanceof LessonsFragment || topFragment instanceof ProgressFragment)
            trainingFragment.setArguments(topFragment.getArguments());

        putFragment(trainingFragment);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (!popFragment())
                super.onBackPressed();
        }
    }

    @Override
    public void onCourseInteraction(Course course, CoursesFragment.CourseInteractionAction action) {
        Bundle bundle = new Bundle();
        bundle.putString("id", course.id);
        Fragment fragment;

        switch (action) {
            case SHOW_LESSONS:
                fragment = new LessonsFragment();
                fragment.setArguments(bundle);
                putFragment(fragment);
                break;
            case SHOW_STATS:
                fragment = new ProgressFragment();
                fragment.setArguments(bundle);
                putFragment(fragment);
                break;
            case SHOW_INFO:
                showCourseInfo(course);
                break;
            case PRACTICE:
                Lesson lesson = CourseManager.i().getNextLesson(this, course);
                if (lesson == null)
                    fragment = new LessonsFragment();
                else
                    fragment = new CourseTrainingFragment();
                fragment.setArguments(bundle);
                putFragment(fragment);
                break;
        }
    }

    @Override
    public void onLessonInteraction(Lesson lesson, LessonsFragment.LessonInteractionAction action) {
        Bundle bundle = new Bundle();
        bundle.putString("id", lesson.course.id);
        bundle.putString("lesson_id", lesson.id);
        Fragment fragment;

        switch (action) {
            case PRACTICE:
                fragment = new LessonTrainingFragment();
                fragment.setArguments(bundle);
                putFragment(fragment);
                break;
        }
    }

    @Override
    public void onTrainingInteraction(TrainingFragment.TrainingInteractionAction action) {
        switch (action) {
            case FINISHED:
                popFragment();
                break;
        }
    }

    private void showCourseInfo(Course course) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Spanned spanned = U.getSpanned(course.description);
        builder.setMessage(spanned)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void putFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .add(R.id.fragment, fragment)
                .addToBackStack(C.BACKSTACK)
                .commit();
        triggerUIUpdates(fm, fragment, +1);

        AnalyticsManager.i().reportFragmentChanged(fragment);
    }

    private boolean popFragment() {
        FragmentManager fm = getSupportFragmentManager();
        int c = fm.getBackStackEntryCount();
        if (c == 0)
            return false;
        fm.popBackStack();
        Fragment fragment = fm.getPrimaryNavigationFragment();
        triggerUIUpdates(fm, fragment, -1);

        AnalyticsManager.i().reportFragmentChanged(fragment);

        return true;
    }

    private void triggerUIUpdates(FragmentManager fm, Fragment fragment, int direction) {
        int c = fm.getBackStackEntryCount() + direction;
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(c > 0);
        actionBar.setDisplayShowHomeEnabled(c > 0);
        findViewById(R.id.main_backdrop).setVisibility(c == 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.onScreenSettings).setVisibility(c == 0 ? View.VISIBLE : View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))
            mViewPermissionsWarning.setVisibility(c == 0 ? View.VISIBLE : View.GONE);

        if (fragment instanceof IUxController)
            //noinspection ResourceType
            mFab.setVisibility(((IUxController) fragment).getFloatingButtonVisibility());
        else
            mFab.setVisibility(View.VISIBLE);
    }

    private void updatePreferences() {
        final SwitchCompat switchUnlock = ((SwitchCompat) findViewById(R.id.switchEnableOnUnlock));
        if (switchUnlock != null)
            switchUnlock.setChecked(new TinyDB(getApplicationContext()).getBoolean(C.SP_ENABLED_ON_UNLOCK, true));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (C.SP_ENABLED_ON_UNLOCK.equals(key))
            updatePreferences();
    }

}
