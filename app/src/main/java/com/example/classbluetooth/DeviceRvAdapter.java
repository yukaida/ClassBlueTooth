package com.example.classbluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceRvAdapter extends RecyclerView.Adapter<DeviceRvAdapter.MyHolder> {
    Context context;
    List<BluetoothDevice> deviceList;
    GetClickPosition getClickPosition;

    public DeviceRvAdapter(Context context, List<BluetoothDevice> list,GetClickPosition getClickPosition) {
        this.context = context;
        this.deviceList = list;
        this.getClickPosition = getClickPosition;
    }

    @NonNull
    @Override
    public DeviceRvAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.ryitem, parent, false);
        MyHolder myHolder = new MyHolder(view);
        return myHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        holder.name.setText(deviceList.get(position).getName());
        holder.address.setText(deviceList.get(position).getAddress());
    }


    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    class MyHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView address;
    
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_name);
            address = itemView.findViewById(R.id.item_address);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getClickPosition.getPosition(getAdapterPosition());
                }
            });
        }
    }
    
    public interface GetClickPosition{
        public void getPosition(int i);
    }
    

}
