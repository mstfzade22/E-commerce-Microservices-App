package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.Notification;

public interface NotificationChannel {
    void send(Notification savedNotification);
}
