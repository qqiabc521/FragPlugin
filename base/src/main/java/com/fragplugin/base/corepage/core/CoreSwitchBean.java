/**
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fragplugin.base.corepage.core;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.fragplugin.base.R;

import java.util.Arrays;


/**
 * 页面跳转控制参数
 */
public class CoreSwitchBean implements Parcelable {
    public static final Creator<CoreSwitchBean> CREATOR = new Creator<CoreSwitchBean>() {
        @Override
        public CoreSwitchBean createFromParcel(Parcel in) {
            return new CoreSwitchBean(in);
        }

        @Override
        public CoreSwitchBean[] newArray(int size) {
            return new CoreSwitchBean[size];
        }
    };
    //页面名
    private String mPageName;
   
    //相关数据
    private Bundle mBundle;
   
    //动画类型
    private int[] mAnim = null;
   
    //是否添加到栈中
    private boolean mAddToBackStack = true;
   
    //是否起新的Activity
    private boolean mNewActivity = false;
   
    //fragment跳转
    private int requestCode = -1;
    
    //fragment跳转使用后是否清理该对象在父容器中的引用对象
    private boolean mResultUsedClear = false;

    public CoreSwitchBean(String pageName) {
        this.mPageName = pageName;
    }

    public CoreSwitchBean(String pageName, Bundle bundle) {
        this.mPageName = pageName;
        this.mBundle = bundle;
    }

    public CoreSwitchBean(String pageName, Bundle bundle, CoreAnim coreAnim) {
        this.mPageName = pageName;
        this.mBundle = bundle;
        this.setAnim(coreAnim);
    }

    public CoreSwitchBean(String pageName, Bundle bundle, int[] anim) {
        this.mPageName = pageName;
        this.mBundle = bundle;
        this.mAnim = anim;
    }

    public CoreSwitchBean(String pageName, Bundle bundle, CoreAnim coreAnim, boolean addToBackStack) {
        this.mPageName = pageName;
        this.mBundle = bundle;
        this.setAnim(coreAnim);
        this.mAddToBackStack = addToBackStack;
    }

    public CoreSwitchBean(String pageName, Bundle bundle, int[] anim, boolean addToBackStack) {
        this.mPageName = pageName;
        this.mBundle = bundle;
        this.mAnim = anim;
        this.mAddToBackStack = addToBackStack;
    }

    public CoreSwitchBean(String pageName, Bundle bundle, CoreAnim coreAnim, boolean addToBackStack, boolean newActivity) {
        this.mPageName = pageName;
        this.mBundle = bundle;
        this.setAnim(coreAnim);
        this.mAddToBackStack = addToBackStack;
        this.mNewActivity = newActivity;
    }
    
    public CoreSwitchBean(String pageName, Bundle bundle, CoreAnim coreAnim, boolean addToBackStack, boolean newActivity, boolean resultUsedClear ) {
        this.mPageName = pageName;
        this.mBundle = bundle;
        this.setAnim(coreAnim);
        this.mAddToBackStack = addToBackStack;
        this.mNewActivity = newActivity;
        this.mResultUsedClear = resultUsedClear;
    }

    public CoreSwitchBean(String pageName, Bundle bundle, int[] anim, boolean addToBackStack, boolean newActivity) {
        this.mPageName = pageName;
        this.mBundle = bundle;
        this.mAnim = anim;
        this.mAddToBackStack = addToBackStack;
        this.mNewActivity = newActivity;
    }

    public CoreSwitchBean(String pageName, Bundle bundle, int[] anim, boolean addToBackStack, boolean newActivity, int requestCode) {
        this.mPageName = pageName;
        this.mBundle = bundle;
        this.mAnim = anim;
        this.mAddToBackStack = addToBackStack;
        this.mNewActivity = newActivity;
        this.requestCode = requestCode;
    }
    
    protected CoreSwitchBean(Parcel in) {
        mPageName = in.readString();
        mBundle = in.readBundle();
        int[] a = {in.readInt(), in.readInt(), in.readInt(), in.readInt()};
        mAnim = a;
        mAddToBackStack = in.readInt() == 1 ? true : false;
        mNewActivity = in.readInt() == 1 ? true : false;
        requestCode = in.readInt();
        mResultUsedClear = in.readInt() == 1?true:false;
    }
    
    public void setAnim(CoreAnim anim) {
        mAnim = convertAnimations(anim);
    }

    /**
     * 动画转化，根据枚举类返回int数组
     *
     * @param coreAnim 动画枚举
     * @return 转化后的动画数组
     */
    public static int[] convertAnimations(CoreAnim coreAnim) {
        if (coreAnim == CoreAnim.present) {
            int[] animations = {R.anim.page_push_in_down, R.anim.page_push_no_ani, R.anim.page_push_no_ani, R.anim.page_push_out_down};
            return animations;
        } else if (coreAnim == CoreAnim.fade) {
            int[] animations = {R.anim.page_alpha_in, R.anim.page_alpha_out, R.anim.page_alpha_in, R.anim.page_alpha_out};
            return animations;
        } else if (coreAnim == CoreAnim.slide) {
            int[] animations = {R.anim.page_slide_in_right, R.anim.page_slide_out_left, R.anim.page_slide_in_left, R.anim.page_slide_out_right};
            return animations;
        }else if (coreAnim == CoreAnim.zoom) {
            int[] animations = {R.anim.page_zoom_in, R.anim.page_zoom_out, R.anim.page_zoom_in, R.anim.page_zoom_out};
            return animations;
        }
        return null;
    }

    public String getPageName() {
        return mPageName;
    }

    public void setPageName(String pageName) {
        mPageName = pageName;
    }

    public boolean isNewActivity() {
        return mNewActivity;
    }

    public void setNewActivity(boolean newActivity) {
        mNewActivity = newActivity;
    }

    public boolean isAddToBackStack() {
        return mAddToBackStack;
    }

    public void setAddToBackStack(boolean addToBackStack) {
        mAddToBackStack = addToBackStack;
    }

    public int[] getAnim() {
        return mAnim;
    }

    public void setAnim(int[] anim) {
        mAnim = anim;
    }

    public Bundle getBundle() {
        return mBundle;
    }

    public void setBundle(Bundle bundle) {
        mBundle = bundle;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }
    
    public boolean isResultUsedClear() {
		return mResultUsedClear;
	}

	public void setResultUsedClear(boolean resultUsedClear) {
		this.mResultUsedClear = resultUsedClear;
	}

	@Override
    public String toString() {
        return "SwitchBean{" +
                "mPageName='" + mPageName + '\'' +
                ", mBundle=" + mBundle +
                ", mAnim=" + Arrays.toString(mAnim) +
                ", mAddToBackStack=" + mAddToBackStack +
                ", mNewActivity=" + mNewActivity +
                ", requestCode=" + requestCode +
                ", mResultUsedClear=" + mResultUsedClear +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        if (mPageName == null) {
            mPageName = "";
        }
        if (mBundle == null) {
            mBundle = new Bundle();
        }
        if (mAnim == null) {
            int[] a = {-1, -1, -1, -1};
            mAnim = a;
        }
        out.writeString(mPageName);
        mBundle.writeToParcel(out, flags);
        if (mAnim != null && mAnim.length == 4) {
            out.writeInt(mAnim[0]);
            out.writeInt(mAnim[1]);
            out.writeInt(mAnim[2]);
            out.writeInt(mAnim[3]);
        } else {
            out.writeInt(-1);
            out.writeInt(-1);
            out.writeInt(-1);
            out.writeInt(-1);
        }
        out.writeInt(mAddToBackStack ? 1 : 0);
        out.writeInt(mNewActivity ? 1 : 0);
        out.writeInt(requestCode);
        out.writeInt(mResultUsedClear ? 1 : 0);
    }


}
