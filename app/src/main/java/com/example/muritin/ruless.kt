package com.example.muritin

class ruless {
/*
    {
        "rules": {
        "users": {
        ".indexOn": ["ownerId"],
        ".read": "auth != null && root.child('users').child(auth.uid).child('role').val() == 'Owner'",
        "$uid": {
        ".read": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && data.child('ownerId').val() == auth.uid))",
        ".write": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && newData.child('role').val() == 'Conductor'))",
        ".validate": "newData.hasChildren(['email', 'name', 'phone', 'age', 'role', 'createdAt'])",
        "phone": {
        ".validate": "newData.val().matches(/^(\\+8801|01)[3-9]\\d{8}$/)"
    },
        "age": {
        ".validate": "newData.isNumber() && newData.val() >= 18 && newData.val() <= 100"
    },
        "email": {
        ".validate": "newData.isString() && newData.val().matches(/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$/)"
    },
        "role": {
        ".validate": "newData.isString() && (newData.val() == 'Rider' || newData.val() == 'Conductor' || newData.val() == 'Owner')"
    },
        "ownerId": {
        ".validate": "newData.val() == null || (newData.isString() && root.child('users').child(newData.val()).child('role').val() == 'Owner')"
    }
    }
    }
    }
    }

 */
}