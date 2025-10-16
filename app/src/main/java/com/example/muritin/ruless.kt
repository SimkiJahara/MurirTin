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
    },
    "buses": {
      ".indexOn": ["ownerId"],
      ".read": "auth != null && root.child('users').child(auth.uid).child('role').val() == 'Owner'",
      "$busId": {
        ".read": "auth != null && (data.child('ownerId').val() == auth.uid || root.child('busAssignments').child($busId).child('conductorId').val() == auth.uid)",
        ".write": "auth != null && (newData.child('ownerId').val() == auth.uid || (data.child('ownerId').val() == auth.uid && !newData.exists()))",
        ".validate": "newData.hasChildren(['busId', 'ownerId', 'name', 'number', 'fitnessCertificate', 'taxToken', 'stops', 'fares', 'createdAt'])",
        "fares": {
          "$stop": {
            ".validate": "$stop.matches(/^[A-Za-z0-9 ]+$/) && newData.hasChildren()",
            "$dest": {
              ".validate": "$dest.matches(/^[A-Za-z0-9 ]+$/) && newData.isNumber() && newData.val() >= 0"
            }
          }
        }
      }
    },
    "busAssignments": {
      ".indexOn": ["conductorId"],
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() == 'Owner' || (query.orderByChild == 'conductorId' && query.equalTo == auth.uid))",
      "$busId": {
        ".read": "auth != null && (root.child('buses').child($busId).child('ownerId').val() == auth.uid || root.child('busAssignments').child($busId).child('conductorId').val() == auth.uid)",
        ".write": "auth != null && root.child('buses').child($busId).child('ownerId').val() == auth.uid",
        ".validate": "newData.hasChild('conductorId') && newData.child('conductorId').isString()"
      }
    },
    "schedules": {
      ".indexOn": ["busId", "conductorId"],
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() == 'Owner' || (query.orderByChild == 'conductorId' && query.equalTo == auth.uid))",
      "$scheduleId": {
        ".read": "auth != null && (root.child('buses').child(data.child('busId').val()).child('ownerId').val() == auth.uid || data.child('conductorId').val() == auth.uid)",
        ".write": "auth != null && newData.child('conductorId').val() == auth.uid && root.child('busAssignments').child(newData.child('busId').val()).child('conductorId').val() == auth.uid",
        ".validate": "newData.hasChildren(['busId', 'conductorId', 'startTime', 'date', 'createdAt']) && newData.child('startTime').isNumber() && newData.child('date').isString() && newData.child('createdAt').isNumber()"
      }
    }
  }
}
 */
}