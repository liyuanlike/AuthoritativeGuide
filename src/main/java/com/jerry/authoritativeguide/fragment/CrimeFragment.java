package com.jerry.authoritativeguide.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.jerry.authoritativeguide.R;
import com.jerry.authoritativeguide.activity.DatePickerActivity;
import com.jerry.authoritativeguide.activity.TimePickerActivity;
import com.jerry.authoritativeguide.modle.Crime;
import com.jerry.authoritativeguide.util.CrimeLab;
import com.jerry.authoritativeguide.util.DeviceUtil;

import java.util.Date;
import java.util.UUID;

/**
 * Created by Jerry on 2016/12/30.
 */

public class CrimeFragment extends Fragment {

    private static final String ARGS_CRIME_ID = "args_crime_id";
    private static final String ARGS_POSITION = "args_crime_position";

    private static final String DIALOG_DATE = "dialog_date";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;

    private Crime mCrime;

    private EditText mTitleEditText;
    private Button mDateButton;
    private Button mTimeButton;
    private CheckBox mSolvedCheckBox;

    private Button mSuspectButton;
    private Button mCallSuspectButton;
    private Button mReportButton;

    /**
     * 通过陋习id创建一个自己的实例
     *
     * @param crimeId
     * @return
     */
    public static Fragment newInstance(UUID crimeId, int position) {
        // 保存陋习id
        Bundle args = new Bundle();
        args.putSerializable(ARGS_CRIME_ID, crimeId);
        args.putInt(ARGS_POSITION, position);

        // 创建实例
        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        // 这里通过Arguments来获取陋习id，从而脱离的activity的限制
        UUID crimeId = (UUID) getArguments().getSerializable(ARGS_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        setResult();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleEditText = (EditText) v.findViewById(R.id.et_title);
        mDateButton = (Button) v.findViewById(R.id.btn_date);
        mTimeButton = (Button) v.findViewById(R.id.btn_time);
        mSolvedCheckBox = (CheckBox) v.findViewById(R.id.cb_solved);
        mSuspectButton = (Button) v.findViewById(R.id.btn_suspect);
        mCallSuspectButton = (Button) v.findViewById(R.id.btn_call_suspect);
        mReportButton = (Button) v.findViewById(R.id.btn_report);

        // 设置标题
        mTitleEditText.setText(mCrime.getTitle());
        mTitleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // 设置日期
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!DeviceUtil.isPad(getActivity())) {
                    DatePickerFragment datePickerFragment = DatePickerFragment.newInstance(mCrime.getDate());
                    datePickerFragment.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                    datePickerFragment.show(getFragmentManager(), DIALOG_DATE);
                } else {
                    Intent intent = DatePickerActivity.newIntent(getActivity(), mCrime.getDate());
                    startActivityForResult(intent, REQUEST_DATE);
                }
            }
        });
        mTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!DeviceUtil.isPad(getActivity())) {
                    TimePickerFragment timePickerFragment = TimePickerFragment.newInstance(mCrime.getDate());
                    timePickerFragment.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                    timePickerFragment.show(getFragmentManager(), DIALOG_DATE);
                } else {
                    Intent intent = TimePickerActivity.newIntent(getActivity(), mCrime.getDate());
                    startActivityForResult(intent, REQUEST_DATE);
                }
            }
        });

        // 设置是否解决
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
            }
        });

        // 嫌疑人
        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(mCrime.getSuspect());
        }
        final Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(intent, REQUEST_CONTACT);
            }
        });

        // 打嫌疑人电话
        if (mCrime.getSuspectPhone() != null) {
            mCallSuspectButton.setEnabled(true);
        }
        mCallSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:1234567890"));
                startActivity(intent);
            }
        });

        // 发送报告
        mReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject));
                intent = Intent.createChooser(intent, getString(R.string.send_report));
                startActivity(intent);
//                ShareCompat.IntentBuilder intentBuilder = ShareCompat.IntentBuilder.from(getActivity());
//                Intent intent = intentBuilder
//                        .setText(getCrimeReport())
//                        .setSubject(getString(R.string.crime_report_subject))
//                        .getIntent();
//                startActivity(intent);
            }
        });

        return v;
    }

    /**
     * 接受从DatePickerFragment传过来的日期数据
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_DATE) {
            Date date = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            // 更新日期数据
            mCrime.setDate(date);
            updateDate();
        } else if (requestCode == REQUEST_CONTACT) {
            // 根据返回结果获取联系人姓名和手机号
            Uri contactUri = data.getData();

            // 获取联系人姓名
            String[] cols = new String[]{ContactsContract.Contacts.DISPLAY_NAME};
            Cursor nameCursor = getActivity().getContentResolver().query(contactUri, cols, null, null, null);
            try {
                if (nameCursor.getCount() != 0) {
                    nameCursor.moveToFirst();
                    // 获取联系人姓名
                    String suspectName = nameCursor.getString(0);
                    mCrime.setSuspect(suspectName);
                    mSuspectButton.setText(suspectName);
                }
            } finally {
                nameCursor.close();
            }

            // 获取联系人手机号
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_delete_crime:
                CrimeLab.get(getActivity()).delete(mCrime);
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 在屏幕失去焦点时更新数据
        CrimeLab.get(getActivity()).update(mCrime);
    }

    /**
     * 更新日期数据
     */
    private void updateDate() {
        mDateButton.setText(mCrime.getDateString());
        mTimeButton.setText(mCrime.getTimeString());
    }

    /**
     * 设置返回的数据
     */
    private void setResult() {
        Intent data = new Intent();
        getActivity().setResult(Activity.RESULT_OK, data);
    }

    /**
     * 拼凑报告信息
     *
     * @return
     */
    private String getCrimeReport() {
        // 是否解决
        String solved;
        if (mCrime.isSolved()) {
            solved = getString(R.string.crime_report_solved);
        } else {
            solved = getString(R.string.crime_report_unsolved);
        }
        // 嫌疑人
        String suspect;
        if (mCrime.getSuspect() == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, mCrime.getSuspect());
        }

        return getString(R.string.crime_report, mCrime.getTitle(), mCrime.getDateString(),
                suspect, solved);
    }
}
