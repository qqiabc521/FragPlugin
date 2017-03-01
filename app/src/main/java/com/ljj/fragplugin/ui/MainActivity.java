package com.ljj.fragplugin.ui;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.fragplugin.base.corepage.CorePageActivity;
import com.fragplugin.base.corepage.core.CoreAnim;
import com.fragplugin.base.corepage.core.CoreSwitchBean;
import com.ljj.fragplugin.R;
import com.ljj.fragplugin.config.AppConfig;

public class MainActivity extends CorePageActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView contentTextView = (TextView) findViewById(R.id.host_content);
        contentTextView.setText(Html.fromHtml("点击以下三个按钮，可分别打开不同apk的页面"));

        findViewById(R.id.home).setOnClickListener(this);
        findViewById(R.id.plugin_item1).setOnClickListener(this);
        findViewById(R.id.plugin_item2).setOnClickListener(this);
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.home:
                CoreSwitchBean page = new CoreSwitchBean(HomeFragment.class.getSimpleName(),null, CoreAnim.slide,true,true);
                openPage(page);
                break;
            case R.id.plugin_item1:
                CoreSwitchBean plugin1Page = new CoreSwitchBean(AppConfig.PLUGIN1_FIRST_FRAGMENT,null, CoreAnim.slide,true,true);
                openPage(plugin1Page);
                break;
            case R.id.plugin_item2:
                CoreSwitchBean plugin2Page = new CoreSwitchBean(AppConfig.PLUGIN2_FIRST_FRAGMENT,null, CoreAnim.slide,true,true);
                openPage(plugin2Page);
                break;
        }

    }
}
