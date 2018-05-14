package ding.bluetoothdemo.controller;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.List;

import ding.bluetoothdemo.R;

/**
 * Created by weiminding on 1/15/18.
 */

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private List<BluetoothDevice> mDeviceList;
    private View.OnClickListener mOnClickListener;
    private Context mContext;
    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener;


    public BluetoothDeviceAdapter(Context context, List<BluetoothDevice> movies,
            CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        mContext = context;
        mDeviceList = movies;
        mOnCheckedChangeListener = onCheckedChangeListener;

    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.row_device_card, parent, false);
        view.setOnClickListener(mOnClickListener);
        return new DeviceViewHolder(view);

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BluetoothDevice device = mDeviceList.get(position);
        boolean isBond = (device.getBondState() == BluetoothDevice.BOND_BONDED);
        ((DeviceViewHolder) holder).cbConnect.setVisibility(isBond ? View.VISIBLE : View.GONE);
        if (isBond) {
            ((DeviceViewHolder) holder).tvName.setTextColor(Color.BLUE);
            ((DeviceViewHolder) holder).tvMac.setTextColor(Color.BLUE);
            setCheckBoxColor(((DeviceViewHolder) holder).cbConnect, Color.BLUE, Color.RED);

            ((DeviceViewHolder) holder).cbConnect.setOnCheckedChangeListener(
                    mOnCheckedChangeListener);
        }

        if (device.getName() == null) {
            ((DeviceViewHolder) holder).tvName.setText("Unknown");
        } else {
            ((DeviceViewHolder) holder).tvName.setText(device.getName());
        }


        ((DeviceViewHolder) holder).tvMac.setText(device.getAddress());

    }

    @Override
    public int getItemCount() {
        return mDeviceList.size();
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }


    class DeviceViewHolder extends RecyclerView.ViewHolder {

        public TextView tvName;
        public TextView tvMac;
        public AppCompatCheckBox cbConnect;

        DeviceViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tv_name);
            tvMac = view.findViewById(R.id.tv_mac);
            cbConnect = view.findViewById(R.id.cb_connect);

        }
    }

    private void setCheckBoxColor(AppCompatCheckBox checkBox, int uncheckedColor,
            int checkedColor) {
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_checked}, // unchecked
                        new int[]{android.R.attr.state_checked}  // checked
                },
                new int[]{
                        uncheckedColor,
                        checkedColor
                }
        );
        checkBox.setButtonTintList(colorStateList);
    }


}
