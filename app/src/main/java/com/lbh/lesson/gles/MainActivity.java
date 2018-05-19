package com.lbh.lesson.gles;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.lbh.lesson.gles.lesson1.TriangleActivity;
import com.lbh.lesson.gles.lesson2.TextureActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        findViewById(R.id.b_triangle).setOnClickListener(this);
        findViewById(R.id.b_texture).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.b_triangle:
                startActivity(new Intent(this, TriangleActivity.class));
                break;
            case R.id.b_texture:
                startActivity(new Intent(this, TextureActivity.class));
                break;
            default:
                break;
        }
    }
}
