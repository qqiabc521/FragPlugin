package com.fragplugin.base.corepage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fragplugin.base.R;
import com.fragplugin.base.corepage.core.CoreAnim;
import com.fragplugin.base.corepage.core.CorePage;
import com.fragplugin.base.corepage.core.CoreSwitchBean;
import com.fragplugin.base.corepage.core.CoreSwitcher;
import com.fragplugin.base.CorePageManager;
import com.fragplugin.base.PluginManager;
import com.fragplugin.base.update.PluginStatusObserve;
import com.fragplugin.base.utils.AppLog;
import com.fragplugin.base.utils.AppApplication;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CorePageActivity extends AppCompatActivity implements CoreSwitcher, CorePageManager.OpenPageHandler,FragmentManager.OnBackStackChangedListener {
    public static final String SWITCH_PAGE = "switch_page";
    public static final String NEW_PAGE = "new_page";
    public static final String START_ACTIVITY_FOR_RESULT = "start_activity_for_result";

    // 日志TAG
    private static final String TAG = CorePage.class.getSimpleName();

    // 应用中所有BaseActivity的引用
    private static List<WeakReference<CorePageActivity>> mActivities = new ArrayList<WeakReference<CorePageActivity>>();

    // 记录首个CoreSwitchBean，用于页面切换
    protected CoreSwitchBean mFirstCoreSwitchBean;

    // 主线程Handler
    private Handler mHandler = null;

    // 当前activity的引用
    private WeakReference<CorePageActivity> mCurrentInstance = null;

    // 是否已经onSaveInstanceState
    private boolean onSaveInstanceStated = false;

    // onSaveInstanceState之后 popStash的个数
    private int onSaveStatePopCount = 0;

    private List<CoreSwitchBean> mPopPages = new ArrayList<CoreSwitchBean>();

    // forresult 的fragment
    private CorePageFragment mFragmentForResult = null;

    // 请求码，必须大于等于0
    private int mFragmentRequestCode = -1;

    private boolean mResultUsedClear = true;

    private PluginStatusObserve mPluginStatusObserve = null;
    /*
     * 保存TouchListener接口的列表
     */
    private ArrayList<FragmentTouchListener> fragmentTouchListeners = new ArrayList<FragmentTouchListener>();

    private static ClassLoader proxyLoader;

    @Override
    public ClassLoader getClassLoader() {
        if (proxyLoader == null) {
            proxyLoader = new ClassLoader(CorePageActivity.class.getClassLoader()) {

                @Override
                protected Class<?> findClass(String className) throws ClassNotFoundException {
                    AppLog.e("ljj", "CorePageActivity findClass == " + className);
                    return CorePageManager.loadPluginClass(className);
                }
            };
        }

        return proxyLoader;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.corepage_activity_base);

        // 获得主线程handler
        mHandler = new Handler(getMainLooper());

        // 当前activity弱引用
        mCurrentInstance = new WeakReference<CorePageActivity>(this);

        // 当前activity增加到activity列表中
        mActivities.add(mCurrentInstance);

        if (AppApplication.getInstance().isDebug()) {
            // 打印所有activity情况
            printAllActivities();
        }

        // 处理新开activity的情况
        if (savedInstanceState == null) {
            // 处理新开activity跳转
            init(getIntent());
        } else {
            AppLog.e(TAG, "onCreate savedInstanceState");

            FragmentManager manager = this.getSupportFragmentManager();
            if (manager != null) {
                int count = manager.getBackStackEntryCount();
                AppLog.e(TAG, "onCreate fragment count : " + count);
                if (count > 0) {
                    String name = manager.getBackStackEntryAt(count - 1).getName();
                    manager.popBackStack(name, 0);
                } else {
                    finishActivity();
                }
            }
            // 恢复数据
            // 需要用注解SaveWithActivity
            loadActivitySavedData(savedInstanceState);
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        onSaveInstanceStated = false;

        // popback掉onSaveInstanceStated之后的Fragment
        popBackStackAfterSavedState();
    }

    /**
     * popBackStack上次在onSaveInstanceState以后的fragment
     */
    private final void popBackStackAfterSavedState() {
        Log.d(TAG, this + " ==> popBackStackAfterSavedState popStashCount=" + onSaveStatePopCount);
        boolean isPopFragment = false;
        while (mPopPages.size() > 0) {
            CoreSwitchBean page = mPopPages.get(0);
            if (page != null) {
                Log.d(TAG, this + " ==> popFragmentInActivity pageName=" + page.getPageName());
                popFragmentInActivity(page.getPageName(), page.getBundle(), this);
                isPopFragment = true;
            }
            mPopPages.remove(page);
        }

        if (isPopFragment) {
            return;
        }

        while (onSaveStatePopCount > 0) {
            Log.d(TAG, "while popStashCount=" + onSaveStatePopCount);
            onSaveStatePopCount = onSaveStatePopCount - 1;
            popBackStack();
        }
    }

    public final boolean isOnSaveInstanceStated() {
        return onSaveInstanceStated;
    }

    public void addPopPage(CoreSwitchBean page) {
        mPopPages.add(page);
    }

    public WeakReference<CorePageActivity> getCurrentInstance() {
        return mCurrentInstance;
    }

    /**
     * 返回最上层的activity
     *
     * @return 栈顶Activity
     */
    public static CorePageActivity getTopActivity() {
        if (mActivities != null) {
            int size = mActivities.size();
            if (size >= 1) {
                WeakReference<CorePageActivity> ref = mActivities.get(size - 1);
                if (ref != null) {
                    return ref.get();
                }
            }
        }
        return null;
    }

    /**
     * 获得当前活动页面名
     *
     * @return 当前页名
     */
    public String getPageName() {
        CorePageFragment frg = getActiveFragment();
        if (frg != null) {
            return frg.getPageName();
        }
        return "";
    }

    /**
     * 弹出页面
     */
    @Override
    public void popPage() {
        popOrFinishActivity();
    }

    /**
     * 保证在主线程操作
     */
    private void popOrFinishActivity() {
        if (this.isFinishing()) {
            return;
        }
        for (int i = 0; i < getSupportFragmentManager().getBackStackEntryCount(); i++) {
            AppLog.i(TAG, this.toString() + " fragment in activity ==> " + getSupportFragmentManager()
                    .getBackStackEntryAt(i).getName());
        }
        if (this.getSupportFragmentManager().getBackStackEntryCount() > 1) {
            if (isMainThread()) {
                popBackStack();
            } else {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        popBackStack();
                    }
                });
            }
        } else {
            finishActivity(this, true);
        }
    }

    private void popBackStack() {
        CorePageFragment fragment = getActiveFragment();
        if (isOnSaveInstanceStated()) {
            Log.d(TAG, fragment.getPageName() + "add popBackStackFragments");
            onSaveStatePopCount++;
            return;
        }

        if (fragment.isVisible() && fragment.isResumed()) {
            this.getSupportFragmentManager().popBackStackImmediate();
        } else {
            this.getSupportFragmentManager().popBackStack();
        }
    }

    /**
     * 是否是主线程
     *
     * @return 是否是主线程
     */
    private boolean isMainThread() {
        return Thread.currentThread() == this.getMainLooper().getThread();
    }

    /**
     * 是否位于栈顶
     *
     * @param fragmentTag fragment的tag
     * @return 指定Fragment是否位于栈顶
     */
    @Override
    public boolean isFragmentTop(String fragmentTag) {
        int size = mActivities.size();
        if (size > 0) {
            WeakReference<CorePageActivity> ref = mActivities.get(size - 1);
            CorePageActivity item = ref.get();
            if (item != null && item == this) {
                FragmentActivity activity = item;
                FragmentManager manager = activity.getSupportFragmentManager();
                if (manager != null) {
                    int count = manager.getBackStackEntryCount();
                    if (count >= 1) {
                        FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(count - 1);
                        if (entry.getName().equalsIgnoreCase(fragmentTag)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isStaticFragmentTop(String fragmentTag) {
        int size = mActivities.size();
        if (size > 0) {
            WeakReference<CorePageActivity> ref = mActivities.get(size - 1);
            CorePageActivity item = ref.get();
            if (item != null) {
                FragmentManager manager = item.getSupportFragmentManager();
                if (manager != null) {
                    int count = manager.getBackStackEntryCount();
                    if (count >= 1) {
                        FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(count - 1);
                        if (entry.getName().equalsIgnoreCase(fragmentTag)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 查找fragment
     *
     * @param pageName page的名字
     * @return 是否找到对应Fragment
     */
    @Override
    public boolean findPage(String pageName) {
        int size = mActivities.size();
        AppLog.e(TAG, "findPage mActivities.size = " + size);
        int j = size - 1;
        boolean hasFind = false;
        for (; j >= 0; j--) {
            WeakReference<CorePageActivity> ref = mActivities.get(j);
            if (ref != null) {
                CorePageActivity item = ref.get();
                if (item == null) {
                    Log.d(TAG, "findPage item is null");
                    continue;
                }
                FragmentManager manager = item.getSupportFragmentManager();
                int count = manager.getBackStackEntryCount();
                for (int i = count - 1; i >= 0; i--) {
                    String name = manager.getBackStackEntryAt(i).getName();
                    if (name.equalsIgnoreCase(pageName)) {
                        hasFind = true;
                        break;
                    }
                }
                if (hasFind) {
                    break;
                }
            }
        }
        return hasFind;
    }

    /**
     * 弹出并用bundle刷新数据，在onFragmentDataReset中回调
     *
     * @param page page的名字
     * @return 跳转到对应的fragment的对象
     */
    @Override
    public Fragment gotoPage(CoreSwitchBean page) {
        if (page == null) {
            Log.e(TAG, "page name empty");
            return null;
        }
        String pageName = page.getPageName();
        if (!findPage(pageName)) {
            Log.d(TAG, "Be sure you have the right pageName" + pageName);
            return this.openPage(page);
        }

        int size = mActivities.size();
        int i = size - 1;
        for (; i >= 0; i--) {// 从activity栈顶开始遍历
            WeakReference<CorePageActivity> ref = mActivities.get(i);
            if (ref != null) {
                CorePageActivity item = ref.get();
                if (item == null) {
                    Log.d(TAG, "gotoPage item null");
                    mActivities.remove(i);
                    continue;
                }

                if (isFragmentInActivity(page.getPageName(), item)) {
                    if (item.isOnSaveInstanceStated()) {
                        item.addPopPage(page);
                    } else {
                        popFragmentInActivity(page.getPageName(), page.getBundle(), item);
                    }
                    break;
                } else {// 清理pageName所在activity栈之上的activity // 找不到就弹出
                    finishActivity(item, true);
                }
            } else {
                mActivities.remove(i);
            }
        }
        return null;
    }

    /**
     * 根据pagename在findAcitivity中是否存在
     *
     * @param pageName
     * @param findAcitivity
     * @return
     */
    public boolean isFragmentInActivity(final String pageName, CorePageActivity findAcitivity) {
        if (pageName == null || findAcitivity == null || findAcitivity.isFinishing()) {
            return false;
        } else {
            final FragmentManager fragmentManager = findAcitivity.getSupportFragmentManager();
            if (fragmentManager != null) {
                Fragment frg = fragmentManager.findFragmentByTag(pageName);
                if (frg != null && frg instanceof CorePageFragment) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 当前activiti中弹fragment
     *
     * @param pageName      page的名字
     * @param bundle        传递的参数
     * @param findAcitivity 当前activity
     * @return 是否弹出成功
     */
    protected boolean popFragmentInActivity(final String pageName, final Bundle bundle, CorePageActivity
            findAcitivity) {
        if (pageName == null || findAcitivity == null || findAcitivity.isFinishing()) {
            return false;
        } else {
            final FragmentManager fragmentManager = findAcitivity.getSupportFragmentManager();
            if (fragmentManager != null) {
                final Fragment frg = fragmentManager.findFragmentByTag(pageName);
                if (frg != null && frg instanceof CorePageFragment) {
                    if (fragmentManager.getBackStackEntryCount() > 1) {
                        if (isMainThread()) {
                            fragmentManager.popBackStack(pageName, 0);
                        } else {
                            this.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    fragmentManager.popBackStack(pageName, 0);
                                }
                            });
                        }
                    }
                    this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ((CorePageFragment) frg).onFragmentDataReset(bundle);
                        }
                    });
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据Switchpage打开activity
     *
     * @param page CoreSwitchBean对象
     */
    public void startActivity(CoreSwitchBean page) {
        try {
            Intent intent = new Intent(this, CorePageActivity.class);
            intent.putExtra(SWITCH_PAGE, page);

            this.startActivity(intent);
            int[] animations = page.getAnim();
            if (animations != null && animations.length >= 2) {
                this.overridePendingTransition(animations[0], animations[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void startActivity(Intent intent) {
        try {
            super.startActivity(intent);
        } catch (Exception e) {
            Log.d(TAG, "startActivity" + e.getMessage());
        }
    }

    /**
     * 根据SwitchBean打开fragment
     *
     * @param page CoreSwitchBean对象
     * @return 打开的Fragment对象
     */
    @Override
    public Fragment openPage(CoreSwitchBean page) {
        boolean addToBackStack = page.isAddToBackStack();
        boolean newActivity = page.isNewActivity();
        Bundle bundle = page.getBundle();

        int[] animations = page.getAnim();
        if (newActivity) {
            startActivity(page);
            return null;
        } else {
            String pageName = page.getPageName();
            return CorePageManager.openPageWithNewFragmentManager(getSupportFragmentManager(),
                    pageName, bundle, animations, addToBackStack, page.getRequestCode(), this, null).getFragment();
        }

    }

    /**
     * 移除无用fragment
     *
     * @param fragmentLists 移除的fragment列表
     */
    @Override
    public void removeUnlessFragment(List<String> fragmentLists) {
        if (this.isFinishing()) {
            return;
        }
        FragmentManager manager = getSupportFragmentManager();
        if (manager != null) {
            FragmentTransaction transaction = manager.beginTransaction();
            int count = manager.getBackStackEntryCount();
            for (int i = 0; i < count; i++) {// TO DEB FOR TEMP
                AppLog.i(TAG, "remove before fragment in activity ==> " + manager.getBackStackEntryAt(i).getName());
            }
            for (String tag : fragmentLists) {
                Fragment fragment = manager.findFragmentByTag(tag);
                if (fragment != null) {
                    transaction.remove(fragment);
                }
            }
            transaction.commitAllowingStateLoss();
            int count1 = manager.getBackStackEntryCount();
            for (int i = 0; i < count1; i++) {// TO DEB FOR TEMP
                AppLog.i(TAG, "remove after fragment in activity ==> " + manager.getBackStackEntryAt(i).getName());
            }
            if (count == 0) {
                finishActivity(this, false);
            }
        }
    }

    /**
     * 给BaseFragment调用
     *
     * @param page     CoreSwitchBean对象
     * @param fragment 要求返回结果的BaseFragment对象
     * @return 打开的fragment对象
     */
    @Override
    public Fragment openPageForResult(CoreSwitchBean page, CorePageFragment fragment) {
        if (page != null) {
            if (page.isNewActivity()) {
                Log.d(TAG, "openPageForResult start new activity-----" + fragment.getPageName());
                mFragmentForResult = fragment;
                mFragmentRequestCode = page.getRequestCode();
                mResultUsedClear = page.isResultUsedClear();
                startActivityForResult(page);
                return null;
            } else {
                String pageName = page.getPageName();
                Bundle bundle = page.getBundle();
                int[] animations = page.getAnim();
                boolean addToBackStack = page.isAddToBackStack();
                FragmentWrap fragmentWrap = CorePageManager.openPageWithNewFragmentManager
                        (getSupportFragmentManager(), pageName,
                                bundle, animations, addToBackStack, page.getRequestCode(), this, fragment == null ? null : fragment.getTagText());
                CorePageFragment frg = (CorePageFragment) fragmentWrap.getFragment();
                if (frg == null) {
                    return null;
                }
                final CorePageFragment opener = fragment;
                frg.setRequestCode(page.getRequestCode());
                frg.setFragmentFinishListener(new CorePageFragment.OnFragmentFinishListener() {
                    @Override
                    public void onFragmentResult(int requestCode, int resultCode, Intent intent) {
                        if (opener == null) {
                            setResult(resultCode, intent);
                        } else {
                            opener.onFragmentResult(requestCode, resultCode, intent);
                        }
                    }
                });
                return frg;
            }
        } else {
            Log.d(TAG, "openPageForResult.SwitchBean is null");
        }
        return null;
    }

    /**
     * @param page CoreSwitchBean对象
     */
    private void startActivityForResult(CoreSwitchBean page) {
        try {
            Intent intent = new Intent(this, CorePageActivity.class);
            intent.putExtra(SWITCH_PAGE, page);
            intent.putExtra(START_ACTIVITY_FOR_RESULT, "true");
            this.startActivityForResult(intent, page.getRequestCode());

            int[] animations = page.getAnim();
            if (animations != null && animations.length >= 2) {
                this.overridePendingTransition(animations[0], animations[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开fragment，并设置是否新开activity，设置是否添加到返回栈
     *
     * @param pageName       页面名
     * @param bundle         参数
     * @param coreAnim       动画
     * @param addToBackStack 返回栈
     * @param newActivity    新activity
     * @return 打开的fragment对象
     */
    public Fragment openPage(String pageName, Bundle bundle, CoreAnim coreAnim, boolean addToBackStack, boolean
            newActivity) {
        CoreSwitchBean page = new CoreSwitchBean(pageName, bundle, coreAnim, addToBackStack, newActivity);
        return openPage(page);
    }

    /**
     * 打开fragment，并设置是否新开activity，设置是否添加到返回栈
     *
     * @param pageName       页面名
     * @param bundle         参数
     * @param anim           动画
     * @param addToBackStack 返回栈
     * @param newActivity    新activity
     * @return 打开的fragment对象
     */
    public Fragment openPage(String pageName, Bundle bundle, int[] anim, boolean addToBackStack, boolean newActivity) {
        CoreSwitchBean page = new CoreSwitchBean(pageName, bundle, anim, addToBackStack, newActivity);
        return openPage(page);
    }

    /**
     * 打开fragment，并设置是否添加到返回栈
     *
     * @param pageName       页面名
     * @param bundle         参数
     * @param coreAnim       动画
     * @param addToBackStack 返回栈
     * @return 打开的fragment对象
     */
    public Fragment openPage(String pageName, Bundle bundle, CoreAnim coreAnim, boolean addToBackStack) {
        CoreSwitchBean page = new CoreSwitchBean(pageName, bundle, coreAnim, addToBackStack);
        return openPage(page);
    }

    /**
     * 打开fragment，并设置是否添加到返回栈
     *
     * @param pageName       页面名
     * @param bundle         参数
     * @param anim           动画
     * @param addToBackStack 返回栈
     * @return 打开的fragment对象
     */
    public Fragment openPage(String pageName, Bundle bundle, int[] anim, boolean addToBackStack) {
        CoreSwitchBean page = new CoreSwitchBean(pageName, bundle, anim, addToBackStack);
        return openPage(page);
    }

    /**
     * 打开fragment
     *
     * @param pageName 页面名
     * @param bundle   参数
     * @param coreAnim 动画
     * @return 打开的fragment对象
     */
    public Fragment openPage(String pageName, Bundle bundle, CoreAnim coreAnim) {
        CoreSwitchBean page = new CoreSwitchBean(pageName, bundle, coreAnim);
        return openPage(page);
    }

    /**
     * 打开fragment
     *
     * @param pageName 页面名
     * @param bundle   参数
     * @param anim     动画
     * @return 打开的fragment对象
     */
    public Fragment openPage(String pageName, Bundle bundle, int[] anim) {
        CoreSwitchBean page = new CoreSwitchBean(pageName, bundle, anim);
        return openPage(page);
    }

    /**
     * 如果是fragment发起的由fragment处理，否则默认处理
     *
     * @param requestCode 请求码
     * @param resultCode  结果码
     * @param data        返回数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult from CorePageActivity, requestCode = " + requestCode + " requestCode = " +
                resultCode);

        boolean tempResultUsedClear = mResultUsedClear;

        if (mFragmentRequestCode == requestCode && mFragmentForResult != null) {
            mFragmentForResult.onFragmentResult(mFragmentRequestCode, resultCode, data);
        } else {
            CorePageFragment fragmentForResult = getActiveFragment();
            if (fragmentForResult != null) {
                fragmentForResult.onFragmentResult(requestCode, resultCode, data);
            }
        }
        if (resultCode != Activity.RESULT_OK || tempResultUsedClear) {
            mFragmentForResult = null;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 如果当前activity中只有一个activity，则关闭activity，否则父类处理
     */
    @Override
    public void onBackPressed() {
        if (this.getSupportFragmentManager().getBackStackEntryCount() == 1) {
            this.finishActivity(this, true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onSaveStatePopCount = 0;
        unregisterPluginStatusObserve();
    }

    /**
     * 如果fragment中处理了则fragment处理否则activity处理
     *
     * @param keyCode keyCode码
     * @param event   KeyEvent对象
     * @return 是否处理时间
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        CorePageFragment activeFragment = getActiveFragment();
        boolean isHanlde = false;
        if (activeFragment != null) {
            isHanlde = activeFragment.onKeyDown(keyCode, event);
        }
        if (!isHanlde) {
            return super.onKeyDown(keyCode, event);
        } else {
            return isHanlde;
        }
    }

    /**
     * 获得当前活动fragmnet
     *
     * @return 当前活动Fragment对象
     */
    public CorePageFragment getActiveFragment() {
        if (this.isFinishing()) {
            return null;
        }
        FragmentManager manager = this.getSupportFragmentManager();
        if (manager != null) {
            int count = manager.getBackStackEntryCount();
            if (count > 0) {
                String tag = manager.getBackStackEntryAt(count - 1).getName();
                return (CorePageFragment) manager.findFragmentByTag(tag);
            }
        }
        return null;
    }

    /**
     * 保存数据
     *
     * @param outState Bundle对象
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.e(TAG, this + " ==> onSaveInstanceState");
        onSaveInstanceStated = true;

        Field[] fields = this.getClass().getDeclaredFields();
        Field.setAccessible(fields, true);
        Annotation[] ans;
        for (Field f : fields) {
            ans = f.getDeclaredAnnotations();
            for (Annotation an : ans) {
                if (an instanceof SaveWithActivity) {
                    try {
                        Object o = f.get(this);
                        if (o == null) {
                            continue;
                        }
                        String fieldName = f.getName();
                        if (o instanceof Integer) {
                            outState.putInt(fieldName, f.getInt(this));
                        } else if (o instanceof String) {
                            outState.putString(fieldName, (String) f.get(this));
                        } else if (o instanceof Long) {
                            outState.putLong(fieldName, f.getLong(this));
                        } else if (o instanceof Short) {
                            outState.putShort(fieldName, f.getShort(this));
                        } else if (o instanceof Boolean) {
                            outState.putBoolean(fieldName, f.getBoolean(this));
                        } else if (o instanceof Byte) {
                            outState.putByte(fieldName, f.getByte(this));
                        } else if (o instanceof Character) {
                            outState.putChar(fieldName, f.getChar(this));
                        } else if (o instanceof CharSequence) {
                            outState.putCharSequence(fieldName, (CharSequence) f.get(this));
                        } else if (o instanceof Float) {
                            outState.putFloat(fieldName, f.getFloat(this));
                        } else if (o instanceof Double) {
                            outState.putDouble(fieldName, f.getDouble(this));
                        } else if (o instanceof String[]) {
                            outState.putStringArray(fieldName, (String[]) f.get(this));
                        } else if (o instanceof Parcelable) {
                            outState.putParcelable(fieldName, (Parcelable) f.get(this));
                        } else if (o instanceof Serializable) {
                            outState.putSerializable(fieldName, (Serializable) f.get(this));
                        } else if (o instanceof Bundle) {
                            outState.putBundle(fieldName, (Bundle) f.get(this));
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        try {
            super.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Log.d(TAG, "startActivityForResult" + e.getMessage());
        }
    }

    /**
     * 恢复数据
     *
     * @param savedInstanceState Bundle对象
     */
    private void loadActivitySavedData(Bundle savedInstanceState) {
        Field[] fields = this.getClass().getDeclaredFields();
        Field.setAccessible(fields, true);
        Annotation[] ans;
        for (Field f : fields) {
            ans = f.getDeclaredAnnotations();
            for (Annotation an : ans) {
                if (an instanceof SaveWithActivity) {
                    try {
                        String fieldName = f.getName();
                        @SuppressWarnings("rawtypes")
                        Class cls = f.getType();
                        if (cls == int.class || cls == Integer.class) {
                            f.setInt(this, savedInstanceState.getInt(fieldName));
                        } else if (String.class.isAssignableFrom(cls)) {
                            f.set(this, savedInstanceState.getString(fieldName));
                        } else if (Serializable.class.isAssignableFrom(cls)) {
                            f.set(this, savedInstanceState.getSerializable(fieldName));
                        } else if (cls == long.class || cls == Long.class) {
                            f.setLong(this, savedInstanceState.getLong(fieldName));
                        } else if (cls == short.class || cls == Short.class) {
                            f.setShort(this, savedInstanceState.getShort(fieldName));
                        } else if (cls == boolean.class || cls == Boolean.class) {
                            f.setBoolean(this, savedInstanceState.getBoolean(fieldName));
                        } else if (cls == byte.class || cls == Byte.class) {
                            f.setByte(this, savedInstanceState.getByte(fieldName));
                        } else if (cls == char.class || cls == Character.class) {
                            f.setChar(this, savedInstanceState.getChar(fieldName));
                        } else if (CharSequence.class.isAssignableFrom(cls)) {
                            f.set(this, savedInstanceState.getCharSequence(fieldName));
                        } else if (cls == float.class || cls == Float.class) {
                            f.setFloat(this, savedInstanceState.getFloat(fieldName));
                        } else if (cls == double.class || cls == Double.class) {
                            f.setDouble(this, savedInstanceState.getDouble(fieldName));
                        } else if (String[].class.isAssignableFrom(cls)) {
                            f.set(this, savedInstanceState.getStringArray(fieldName));
                        } else if (Parcelable.class.isAssignableFrom(cls)) {
                            f.set(this, savedInstanceState.getParcelable(fieldName));
                        } else if (Bundle.class.isAssignableFrom(cls)) {
                            f.set(this, savedInstanceState.getBundle(fieldName));
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 打印，调试用
     */
    private void printAllActivities() {
        int size = mActivities.size();
        Log.d(TAG, "------------CorePageActivity print all------------activities size:" + size);

        for (int i = 0; i < size; i++) {
            WeakReference<CorePageActivity> ref = mActivities.get(i);
            if (ref != null) {
                CorePageActivity item = ref.get();
                if (item != null) {
                    Log.d(TAG, "------------CorePageActivity index:" + i);
                    FragmentManager manager = item.getSupportFragmentManager();
                    if (manager != null) {
                        int count = manager.getBackStackEntryCount();
                        for (int j = 0; j < count; j++) {
                            String name = manager.getBackStackEntryAt(j).getName();
                            Log.d(TAG, "------------CorePageActivity fragment" + i + " ,fragmentName = " + name);
                        }
                    }
                }
            }
        }
    }

    /**
     * 初始化intent
     *
     * @param mNewIntent Intent对象
     */
    private void init(Intent mNewIntent) {
        try {
            CoreSwitchBean page = mNewIntent.getParcelableExtra(SWITCH_PAGE);
            String startActivityForResult = mNewIntent.getStringExtra(START_ACTIVITY_FOR_RESULT);
            boolean newPage = mNewIntent.getBooleanExtra(NEW_PAGE, true);
            if (!newPage) {
                gotoPage(page);
                finishActivity(this, false);
            } else {
                this.mFirstCoreSwitchBean = page;
                if (page != null) {
                    boolean addToBackStack = page.isAddToBackStack();
                    String pageName = page.getPageName();
                    Bundle bundle = page.getBundle();

                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.addOnBackStackChangedListener(this);

                    FragmentWrap fragmentWrap = CorePageManager.openPageWithNewFragmentManager(getSupportFragmentManager(),
                            pageName,
                            bundle,
                            null,
                            addToBackStack,
                            page.getRequestCode(),
                            this, null);

                    if (fragmentWrap.getStatus() == FragmentWrap.LOADING) {
                        return;
                    }
                    CorePageFragment fragment = (CorePageFragment) fragmentWrap.getFragment();
                    if (fragment != null) {
                        if ("true".equalsIgnoreCase(startActivityForResult)) {
                            fragment.setRequestCode(page.getRequestCode());
                            fragment.setFragmentFinishListener(new CorePageFragment.OnFragmentFinishListener() {
                                @Override
                                public void onFragmentResult(int requestCode, int resultCode, Intent intent) {
                                    CorePageActivity.this.setResult(resultCode, intent);
                                }
                            });
                        }
                    } else {
                        finishActivity(this, false);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
            finishActivity(this, false);
        }
    }

    @Override
    public void finishActivity() {
        finishActivity(this, true);
    }

    @Override
    public void finishActivity(boolean showAnimation) {
        finishActivity(this, showAnimation);
    }

    /**
     * 结束activity，设置是否显示动画
     *
     * @param activity      BaseActivity对象
     * @param showAnimation 是否显示动画
     */
    private void finishActivity(CorePageActivity activity, boolean showAnimation) {
        if (activity != null) {
            Log.d(TAG, "finishActivity : " + activity.toString());
            mActivities.remove(activity.getCurrentInstance());
            activity.finish();
            // 从activity列表中移除当前实例
        }
        if (showAnimation) {
            // 动画
            int[] animations = null;
            if (activity.mFirstCoreSwitchBean != null && activity.mFirstCoreSwitchBean.getAnim() != null) {
                animations = activity.mFirstCoreSwitchBean.getAnim();
            }
            if (animations != null && animations.length >= 4) {
                overridePendingTransition(animations[2], animations[3]);
            }
        }
    }

    /**
     * Called whenever the contents of the back stack change.
     */
    @Override
    public void onBackStackChanged() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if(fragmentManager.getBackStackEntryCount() > 0){
            CorePageFragment topFragment = getActiveFragment();
            if(topFragment != null){
                topFragment.onResume();
            }
        }
    }

    /**
     * 注解了该注解数据会被保存
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface SaveWithActivity {
    }

    public interface FragmentTouchListener {
        public boolean onTouchEvent(MotionEvent event);
    }

    /**
     * 提供给Fragment通过getActivity()方法来注册自己的触摸事件的方法
     *
     * @param listener
     */
    public void registerMyTouchListener(FragmentTouchListener listener) {
        fragmentTouchListeners.add(listener);
    }

    /**
     * 提供给Fragment通过getActivity()方法来取消注册自己的触摸事件的方法
     *
     * @param listener
     */
    public void unRegisterMyTouchListener(FragmentTouchListener listener) {
        fragmentTouchListeners.remove(listener);
    }

    /**
     * 分发触摸事件给所有注册了TouchListener的接口
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        for (FragmentTouchListener listener : fragmentTouchListeners) {
            listener.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 清理所有的activity
     *
     * @param retainTop 是否保留顶部activity
     */
    @Override
    public void clearAllActivity(boolean retainTop) {
        Log.d(TAG, "clearAllActivity start mActivities.size() = " + mActivities.size());
        int retainCount = retainTop ? 1 : 0;
        while (mActivities.size() > retainCount) {
            WeakReference<CorePageActivity> ref = mActivities.get(0);
            if (ref != null) {
                CorePageActivity item = ref.get();
                if (item == null) {
                    Log.d(TAG, "clearAllActivity item null");
                    mActivities.remove(ref);
                    continue;
                }
                finishActivity(item, false);
            }
        }
        Log.d(TAG, "clearAllActivity end mActivities.size() = " + mActivities.size());
    }

    public static void clearAll() {
        Log.d(TAG, "clearAll start mActivities.size() = " + mActivities.size());
        while (mActivities.size() > 0) {
            WeakReference<CorePageActivity> ref = mActivities.get(0);
            if (ref != null) {
                CorePageActivity item = ref.get();
                if (item == null) {
                    Log.d(TAG, "clearAll item null");
                    mActivities.remove(ref);
                    continue;
                }
                mActivities.remove(item.getCurrentInstance());
                item.finish();
            }
        }
        Log.d(TAG, "clearAll end mActivities.size() = " + mActivities.size());
    }

    @Override
    public void synaLoadPlugin(final String pluginName, final String pageName, final Bundle bundle, final int[]
            animations, final boolean addToBackStack, final int requestCode, final String openPageName) {
        final View loadRootView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.activity_base_plugin, null);
        final TextView pluginTv = (TextView) loadRootView.findViewById(R.id.plugin_tv);
        final TextView pluginReasonTv = (TextView) loadRootView.findViewById(R.id.plugin_reason_tv);
        final ImageView pluginStatusIv = (ImageView) loadRootView.findViewById(R.id.plugin_status_iv);

        mPluginStatusObserve = new PluginStatusObserve(pluginName, loadRootView) {
            @Override
            public void onPrepare() {
                ((ViewGroup) findViewById(R.id.fragment_container)).addView(loadRootView, new ViewGroup.LayoutParams
                        (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                pluginReasonTv.setText("");
                pluginStatusIv.setImageResource(R.drawable.comm_loading_dialog_b_anim);
                ((AnimationDrawable) pluginStatusIv.getDrawable()).start();
            }

            @Override
            public void onDownloading(int process) {
                pluginTv.setText(String.format(getString(R.string.plugin_loading), process));
            }

            @Override
            public void onSuccess() {
                unregisterPluginStatusObserve();

                ((ViewGroup) findViewById(R.id.fragment_container)).removeView(loadRootView);
                CoreSwitchBean page = new CoreSwitchBean(pageName, bundle, animations, addToBackStack, false);
                page.setRequestCode(requestCode);

                if (requestCode == -1) {
                    openPage(page);
                } else {
                    CorePageFragment fragment = null;
                    if(openPageName != null){
                        fragment = (CorePageFragment) getSupportFragmentManager().findFragmentByTag(openPageName);
                    }

                    openPageForResult(page, fragment);
                }
            }

            @Override
            public void onFaile(String msg) {
                unregisterPluginStatusObserve();
                if (pluginStatusIv.getDrawable() instanceof AnimationDrawable) {
                    ((AnimationDrawable) pluginStatusIv.getDrawable()).stop();
                }
                pluginStatusIv.setImageResource(R.mipmap.comm_plugin_refresh);
                pluginTv.setText(getString(R.string.plugin_faile));
                pluginReasonTv.setText(String.format(getString(R.string.plugin_faile_reason),msg));

                loadRootView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((ViewGroup) findViewById(R.id.fragment_container)).removeView(loadRootView);
                        synaLoadPlugin(pluginName,pageName,bundle,animations,addToBackStack,requestCode, null);
                    }
                });
            }
        };

        PluginManager.loadPluginAsync(getApplicationContext(),pluginName, mPluginStatusObserve, true);
    }

    private void unregisterPluginStatusObserve(){
        if (mPluginStatusObserve != null) {
            PluginManager.unregisterPluginStatusObserve(mPluginStatusObserve);
            mPluginStatusObserve = null;
        }
    }

}
