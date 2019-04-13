package com.example.social;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import java.util.ArrayList;

public class NotificationsFragment extends Fragment {

    private ArrayList<Notification> notifications;

    public NotificationsFragment() {
        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public NotificationsFragment(ArrayList<Notification> notifications){
        this.notifications = notifications;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        ListView notificationsList = view.findViewById(R.id.notificationsList);
        NotificationAdapter adapter = new NotificationAdapter(notifications ,getActivity() );
        notificationsList.setAdapter(adapter);

        return view;
    }
}