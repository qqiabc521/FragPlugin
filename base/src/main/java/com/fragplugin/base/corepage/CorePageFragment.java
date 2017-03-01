package com.fragplugin.base.corepage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;

import com.fragplugin.base.corepage.core.CoreAnim;
import com.fragplugin.base.corepage.core.CorePage;
import com.fragplugin.base.corepage.core.CoreSwitchBean;
import com.fragplugin.base.corepage.core.CoreSwitcher;
import com.fragplugin.base.CorePageManager;
import com.fragplugin.base.utils.AppLog;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

public class CorePageFragment extends Fragment {

	private static final String TAG = CorePage.class.getSimpleName();
	// 日志tag
	private String mPageName;
	// 页面名
	private int mRequestCode;
	// 用于startForResult的requestCode
	private CoreSwitcher mPageCoreSwitcher;
	// openPageForResult接口，用于传递返回结果
	private OnFragmentFinishListener mFragmentFinishListener;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface SaveWithFragment {

	}

	protected void loadData(Bundle savedInstanceState) {
		Field[] fields = this.getClass().getDeclaredFields();
		Field.setAccessible(fields, true);
		Annotation[] ans;
		for (Field f : fields) {
			ans = f.getDeclaredAnnotations();
			for (Annotation an : ans) {
				if (an instanceof SaveWithFragment) {
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
	 * 设置该接口用于返回结果
	 *
	 * @param listener
	 *            OnFragmentFinishListener对象
	 */
	public void setFragmentFinishListener(OnFragmentFinishListener listener) {
		this.mFragmentFinishListener = listener;
	}

	/**
	 * 设置openPageForResult打开的页面的返回结果
	 *
	 * @param resultCode
	 *            返回结果码
	 * @param intent
	 *            返回的intent对象
	 */
	public void setFragmentResult(int resultCode, Intent intent) {
		if (mFragmentFinishListener != null) {
			mFragmentFinishListener.onFragmentResult(mRequestCode, resultCode, intent);
		}
	}

	/**
	 * 得到requestCode
	 *
	 * @return 请求码
	 */
	public int getRequestCode() {
		return this.mRequestCode;
	}

	/**
	 * 设置requestCode
	 *
	 * @param code
	 *            请求码
	 */
	public void setRequestCode(int code) {
		this.mRequestCode = code;
	}

	/**
	 * 将Activity中onKeyDown在Fragment中实现，
	 *
	 * @param keyCode
	 *            keyCode码
	 * @param event
	 *            KeyEvent对象
	 * @return 是否处理
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}

	/**
	 * 数据设置，回调
	 *
	 * @param bundle
	 *            刷新数据的Bundle对象
	 */
	public void onFragmentDataReset(Bundle bundle) {
		Log.d(TAG, getPageName() +" onFragmentDataReset");

	}

	public interface PopCallback {
		void run();
	}

	/**
	 * 用于openPageForResult，获得返回内容后需要再次调openPage的场景，只适合不新开Activity的情况， 如果新开activity请像Activity返回结果那样操作
	 *
	 * @param callback
	 *            返回码
	 */
	public void popToBackForResult(PopCallback callback) {
		this.popToBack(null, null);
		callback.run();
	}

	/**
	 * 弹出栈顶的Fragment。如果Activity中只有一个Fragemnt时，Acitivity也退出。
	 */
	public void popToBack() {
		this.popToBack(null, null);
	}

	/**
	 * 如果在fragment栈中找到，则跳转到该fragment中去，否则弹出栈顶
	 *
	 * @param pageName
	 *            页面名
	 * @param bundle
	 *            参数
	 */
	public final void popToBack(String pageName, Bundle bundle) {
		CoreSwitcher coreSwitcher = getSwitcher();
		if (coreSwitcher != null) {
			if (pageName == null) {
				coreSwitcher.popPage();
			} else {
				if (this.findPage(pageName)) {
					Log.d(TAG, "popToBack :" + pageName);
					CoreSwitchBean page = new CoreSwitchBean(pageName, bundle);
					coreSwitcher.gotoPage(page);
				} else {
					coreSwitcher.popPage();
				}
			}
		} else {
			Log.d(TAG, "pageSwitcher null");
		}
	}

	/**
	 * 得到页面切换Switcher
	 *
	 * @return 页面切换Switcher
	 */
	public CoreSwitcher getSwitcher() {
		synchronized (CorePageFragment.this) {// 加强保护，保证pageSwitcher 不为null
			if (mPageCoreSwitcher == null) {
				Activity activity = getActivity();
				if (activity != null && activity instanceof CoreSwitcher) {
					mPageCoreSwitcher = (CoreSwitcher) activity;
				}
				if (mPageCoreSwitcher == null) {
					CorePageActivity topActivity = CorePageActivity.getTopActivity();
					if (topActivity != null && topActivity instanceof CoreSwitcher) {
						mPageCoreSwitcher = (CoreSwitcher) topActivity;
					}
				}
			}
		}
		return mPageCoreSwitcher;
	}

	/**
	 * 设置Switcher
	 *
	 * @param pageCoreSwitcher
	 *            CoreSwitcher对象
	 */
	public void setSwitcher(CoreSwitcher pageCoreSwitcher) {
		this.mPageCoreSwitcher = pageCoreSwitcher;
	}

	/**
	 * 查找fragment是否存在，通过Switcher查找
	 *
	 * @param pageName
	 *            页面名
	 * @return 是否找到
	 */
	public boolean findPage(String pageName) {
		if (pageName == null) {
			Log.d(TAG, "pageName is null");
			return false;
		}
		CoreSwitcher coreSwitcher = getSwitcher();
		if (coreSwitcher != null) {
			return coreSwitcher.findPage(pageName);
		} else {
			Log.d(TAG, "pageSwitch is null");
			return false;
		}

	}

	/**
	 * 对应fragment是否位于栈顶，通过Switcher查找
	 *
	 * @param fragmentTag
	 *            fragment的tag
	 * @return 是否位于栈顶
	 */
	public boolean isFragmentTop(String fragmentTag) {
		CoreSwitcher pageCoreSwitcher = this.getSwitcher();
		if (pageCoreSwitcher != null) {
			return pageCoreSwitcher.isFragmentTop(fragmentTag);

		} else {
			Log.d(TAG, "pageSwitcher is null");
			return false;
		}
	}

	/**
	 * 重新该方法用于获得返回的数据
	 *
	 * @param requestCode
	 *            请求码
	 * @param resultCode
	 *            返回结果码
	 * @param data
	 *            返回数据
	 */
	public void onFragmentResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onFragmentResult from baseFragment：requestCode-" + requestCode + "  resultCode-" + resultCode);
	}

	/**
	 * 结束fragment所在的当前activity
	 */
	protected final void finishActivity() {
		CoreSwitcher pageCoreSwitcher = this.getSwitcher();
		if (pageCoreSwitcher != null) {
			pageCoreSwitcher.finishActivity();
		} else {
			Log.d(TAG, "pageSwitcher is null");
		}
	}

	/**
	 * 结束fragment所在的当前activity
	 * @param showAnimation 是否显示退场动画
	 */
	protected final void finishActivity(boolean showAnimation) {
		CoreSwitcher pageCoreSwitcher = this.getSwitcher();
		if (pageCoreSwitcher != null) {
			pageCoreSwitcher.finishActivity(showAnimation);
		} else {
			Log.d(TAG, "pageSwitcher is null");
		}
	}

	/**
	 * 在当前activity中打开一个fragment，并添加到返回栈中(默认使用滑动动画)
	 *
	 * @param pageName
	 *            Fragemnt 名，在page.json中配置。
	 * @param bundle
	 *            页面跳转时传递的参数
	 * @return 打开的fragment对象
	 */
	public final Fragment openPage(String pageName, Bundle bundle) {
		return this.openPage(pageName, bundle, CoreSwitchBean.convertAnimations(CoreAnim.slide), true);
	}

	/**
	 * 在当前activity中打开一个fragment，并添加到返回栈中
	 *
	 * @param pageName
	 *            Fragemnt 名，在page.json中配置。
	 * @param bundle
	 *            页面跳转时传递的参数
	 * @param coreAnim
	 *            指定的动画理性 none/slide(左右平移)/present(由下向上)/fade(fade 动画)
	 * @return 打开的fragment对象
	 */
	public final Fragment openPage(String pageName, Bundle bundle, CoreAnim coreAnim) {
		return this.openPage(pageName, bundle, CoreSwitchBean.convertAnimations(coreAnim), true);
	}

	/**
	 * 在当前activity中打开一个fragment，并设置是否添加到返回栈
	 *
	 * @param pageName
	 *            Fragemnt 名，在page.json中配置。
	 * @param bundle
	 *            页面跳转时传递的参数
	 * @param anim
	 *            指定的动画理性 none/slide(左右平移)/present(由下向上)/fade(fade 动画)
	 * @param addToBackStack
	 *            是否添加到用户操作栈中
	 * @return 打开的fragment对象
	 */
	public final Fragment openPage(String pageName, Bundle bundle, int[] anim, boolean addToBackStack) {
		return this.openPage(pageName, bundle, anim, addToBackStack, false);
	}

	public final Fragment findPageByName(String pageName) {
		return CorePageManager.findFragmentByName(pageName);
	}

	/**
	 * 打开一个fragment并设置是否新开activity，设置是否添加返回栈
	 *
	 * @param pageName
	 *            Fragemnt 名，在page.json中配置。
	 * @param bundle
	 *            页面跳转时传递的参数
	 * @param anim
	 *            指定的动画理性 none/slide(左右平移)/present(由下向上)/fade(fade 动画)
	 * @param addToBackStack
	 *            是否添加到用户操作栈中
	 * @param newActivity
	 *            该页面是否新建一个Activity
	 * @return 打开的fragment对象
	 */
	public final Fragment openPage(String pageName, Bundle bundle, int[] anim, boolean addToBackStack, boolean newActivity) {
		if (pageName == null) {
			Log.d(TAG, "pageName is null");
			return null;
		}
		CoreSwitcher coreSwitcher = this.getSwitcher();
		if (coreSwitcher != null) {
			CoreSwitchBean page = new CoreSwitchBean(pageName, bundle, anim, addToBackStack, newActivity);
			return coreSwitcher.openPage(page);
		} else {
			Log.d(TAG, "pageSwitcher is null");
			return null;
		}
	}

	/**
	 * 在当前activity中打开一个fragment，并添加到返回栈中
	 *
	 * @param pageName
	 *            Fragemnt 名，在page.json中配置。
	 * @param bundle
	 *            页面跳转时传递的参数
	 * @param anim
	 *            指定的动画理性 none/slide(左右平移)/present(由下向上)/fade(fade 动画)
	 * @return 打开的fragment对象
	 */
	public final Fragment openPage(String pageName, Bundle bundle, int[] anim) {
		return this.openPage(pageName, bundle, anim, true);
	}

	/**
	 * 在当前activity中打开一个fragment，并设置是否添加到返回栈
	 *
	 * @param pageName
	 *            Fragemnt 名，在page.json中配置。
	 * @param bundle
	 *            页面跳转时传递的参数
	 * @param coreAnim
	 *            指定的动画理性 none/slide(左右平移)/present(由下向上)/fade(fade 动画)
	 * @param addToBackStack
	 *            是否添加到用户操作栈中
	 * @return 打开的fragment对象
	 */
	public final Fragment openPage(String pageName, Bundle bundle, CoreAnim coreAnim, boolean addToBackStack) {
		return this.openPage(pageName, bundle, CoreSwitchBean.convertAnimations(coreAnim), addToBackStack, false);
	}

	/**
	 * 打开一个fragment并设置是否新开activity，设置是否添加返回栈
	 *
	 * @param pageName
	 *            Fragemnt 名，在page.json中配置。
	 * @param bundle
	 *            页面跳转时传递的参数
	 * @param coreAnim
	 *            指定的动画理性 none/slide(左右平移)/present(由下向上)/fade(fade 动画)
	 * @param addToBackStack
	 *            是否添加到用户操作栈中
	 * @param newActivity
	 *            该页面是否新建一个Activity
	 * @return 打开的fragment对象
	 */
	public final Fragment openPage(String pageName, Bundle bundle, CoreAnim coreAnim, boolean addToBackStack, boolean newActivity) {
		return this.openPage(pageName, bundle, CoreSwitchBean.convertAnimations(coreAnim), addToBackStack, newActivity);
	}

	/**
	 * @param pageName
	 *            页面名
	 * @param bundle
	 *            参数
	 * @param coreAnim
	 *            动画
	 * @return 打开的fragment对象
	 */
	public Fragment gotoPage(String pageName, Bundle bundle, CoreAnim coreAnim) {
		return this.gotoPage(pageName, bundle, coreAnim, false);

	}

	/**
	 * 新建或跳转到一个页面（Fragment）。找不到pageName Fragment时，就新建Fragment。找到pageName Fragment时,则弹出该Fragement到栈顶上的所有actvity和fragment
	 *
	 * @param pageName
	 *            Fragemnt 名，在在page.json中配置。
	 * @param bundle
	 *            页面跳转时传递的参数
	 * @param coreAnim
	 *            指定的动画理性 none/slide(左右平移)/present(由下向上)/fade(fade 动画)
	 * @param newActivity
	 *            该页面是否新建一个Activity
	 * @return 打开的fragment对象
	 */
	public Fragment gotoPage(String pageName, Bundle bundle, CoreAnim coreAnim, boolean newActivity) {
		CoreSwitcher pageCoreSwitcher = this.getSwitcher();
		if (pageCoreSwitcher != null) {
			CoreSwitchBean page = new CoreSwitchBean(pageName, bundle, coreAnim, true, newActivity);
			return pageCoreSwitcher.gotoPage(page);
		} else {

			Log.d(TAG, "pageSwitcher is null");
			return null;
		}
	}

	/**
	 * 打开fragment并请求获得返回值
	 *
	 * @param pageName
	 *            页面名
	 * @param bundle
	 *            参数
	 * @param coreAnim
	 *            动画
	 * @param requestCode
	 *            请求码
	 * @return 打开的fragment对象
	 */
	public final Fragment openPageForResult(String pageName, Bundle bundle, CoreAnim coreAnim, int requestCode) {
		return this.openPageForResult(false, pageName, bundle, coreAnim, requestCode);
	}

	/**
	 * 打开fragment并请求获得返回值,并设置是否在新activity中打开
	 *
	 * @param newActivity
	 *            是否新开activity
	 * @param pageName
	 *            页面名
	 * @param bundle
	 *            参数
	 * @param coreAnim
	 *            动画
	 * @param requestCode
	 *            请求码
	 * @return 打开的fragment对象
	 */
	public final Fragment openPageForResult(boolean newActivity, String pageName, Bundle bundle, CoreAnim coreAnim, int requestCode) {
		return this.openPageForResult(newActivity, pageName, bundle, coreAnim, requestCode, true);
	}

	public final Fragment openPageForResult(boolean newActivity, String pageName, Bundle bundle, CoreAnim coreAnim, int requestCode, boolean resultUsedClear) {

		CoreSwitcher pageCoreSwitcher = this.getSwitcher();
		if (pageCoreSwitcher != null) {
			CoreSwitchBean page = new CoreSwitchBean(pageName, bundle, coreAnim, true, newActivity, resultUsedClear);
			page.setRequestCode(requestCode);

			return pageCoreSwitcher.openPageForResult(page, this);
		} else {
			Log.d(TAG, "pageSwitcher is null");
			return null;
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getPageName() != null) {
			Log.d(TAG, "====Fragment.onCreate====" + getPageName());
		}
	}

	public CorePageFragment getActiveChildFragment(){
		if (this.isDetached()) {
			return null;
		}
		FragmentManager manager = this.getChildFragmentManager();
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
	 * 获得页面名
	 *
	 * @return 页面名
	 */
	public String getPageName() {
		return mPageName;
	}

	/**
	 * 设置页面名
	 *
	 * @param pageName
	 *            页面名
	 */
	public void setPageName(String pageName) {
		mPageName = pageName;
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//TODO 內存泄漏检测，release版本需要删除
//		RefWatcher refWatcher = BangbangApplication.getInstance().getRefWatcher();
//		refWatcher.watch(this);
	}

	/**
	 * Fragment tag
	 *
	 * @return
	 */
	public String getTagText() {
		return this.getClass().getSimpleName();
	}

	public void printStash(){
		AppLog.e("ljj", this.getPageName()+" isAdded:"+this.isAdded());
		AppLog.e("ljj", this.getPageName()+" isHidden:"+this.isHidden());
		AppLog.e("ljj", this.getPageName()+" isDetached:"+this.isDetached());
		AppLog.e("ljj", this.getPageName()+" isInLayout:"+this.isInLayout());
		AppLog.e("ljj", this.getPageName()+" isResumed:"+this.isResumed());
		AppLog.e("ljj", this.getPageName()+" isRemoving:"+this.isRemoving());
		AppLog.e("ljj", this.getPageName()+" isVisible:"+this.isVisible());
	}

	/**
	 * 页面跳转接口
	 */
	public interface OnFragmentFinishListener {
		void onFragmentResult(int requestCode, int resultCode, Intent intent);
	}

}
