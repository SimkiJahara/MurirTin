package com.example.muritin

class ruless {
/*

{
  "rules": {
    "users": {
      ".indexOn": ["ownerId"],
      ".read": "auth != null && root.child('users').child(auth.uid).child('role').val() == 'Owner'",
      "$uid": {
        ".read": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && data.child('ownerId').val() == auth.uid) || root.child('users').child(auth.uid).child('role').val() == 'Conductor' || root.child('users').child(auth.uid).child('role').val() == 'Rider')",
        ".write": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && newData.child('role').val() == 'Conductor'))",
        ".validate": "newData.hasChildren(['email', 'name', 'phone', 'nid', 'age', 'role', 'createdAt'])"
      }
    },
    "buses": {
      ".indexOn": ["ownerId", "busId"],
      ".read": "auth != null",
      "$busId": {
        ".read": "auth != null",
        ".write": "auth != null && (newData.child('ownerId').val() == auth.uid || (data.child('ownerId').val() == auth.uid && !newData.exists()))"
      }
    },
    "busAssignments": {
      ".indexOn": ["conductorId"],
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() == 'Owner' || (query.orderByChild == 'conductorId' && query.equalTo == auth.uid))",
      "$busId": {
        ".read": "auth != null && (root.child('buses').child($busId).child('ownerId').val() == auth.uid || root.child('busAssignments').child($busId).child('conductorId').val() == auth.uid)",
        ".write": "auth != null && root.child('buses').child($busId).child('ownerId').val() == auth.uid || root.child('users').child(auth.uid).child('role').val() == 'Conductor'"
      }
    },
    "schedules": {
      ".indexOn": ["busId", "conductorId", "startTime", "endTime"],
      ".read": "auth != null",
      "$scheduleId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "requests": {
      ".indexOn": ["status", "riderId", "acceptedBy", "busId", "conductorId"],
      ".read": "auth != null",
      "$id": {
        ".read": "auth != null",
        ".write": "auth != null",
        ".validate": "((newData.child('status').val() == 'Cancelled' && data.child('status').val() == 'Pending' && newData.child('riderId').val() == data.child('riderId').val()) || (newData.child('status').val() == 'Completed' && (data.child('status').val() == 'Accepted' || data.child('status').val() == 'Completed')) || (newData.hasChildren(['id', 'riderId', 'pickup', 'destination', 'pickupLatLng', 'destinationLatLng', 'seats', 'fare', 'status', 'createdAt', 'requestedRoute'])))",
        "rating": {
          ".read": "auth != null",
          ".write": "auth != null && data.parent().child('riderId').val() == auth.uid && (data.parent().child('status').val() == 'Accepted' || data.parent().child('status').val() == 'Completed') && !data.exists()"
        },
        "rideStatus": {
          ".read": "auth != null && (data.parent().child('riderId').val() == auth.uid || data.parent().child('conductorId').val() == auth.uid)",
          ".write": "auth != null && (data.parent().child('riderId').val() == auth.uid || data.parent().child('conductorId').val() == auth.uid)",
          "otpVerified": {
            ".validate": "newData.isBoolean()"
          },
          "inBusTravelling": {
            ".validate": "newData.isBoolean()"
          },
          "boardedAt": {
            ".validate": "newData.isNumber()"
          },
          "earlyExitRequested": {
            ".validate": "newData.isBoolean()"
          },
          "lateExitRequested": {
            ".validate": "newData.isBoolean()"
          },
          "riderArrivedConfirmed": {
            ".validate": "newData.isBoolean()"
          },
          "conductorArrivedConfirmed": {
            ".validate": "newData.isBoolean()"
          },
          "fareCollected": {
            ".validate": "newData.isBoolean()"
          },
          "actualFare": {
            ".validate": "newData.isNumber() && newData.val() >= 0"
          },
          "tripCompleted": {
            ".validate": "newData.isBoolean()"
          },
          "tripCompletedAt": {
            ".validate": "newData.isNumber()"
          },
          "riderArrivedAt": {
            ".validate": "newData.isNumber()"
          },
          "conductorArrivedAt": {
            ".validate": "newData.isNumber()"
          },
          "fareCollectedAt": {
            ".validate": "newData.isNumber()"
          },
          "otpVerifiedAt": {
            ".validate": "newData.isNumber()"
          },
          "earlyExitRequestedAt": {
            ".validate": "newData.isNumber()"
          },
          "earlyExitStop": {
            ".validate": "newData.isString()"
          },
          "earlyExitLatLng": {
            ".validate": "newData.hasChildren(['lat', 'lng'])"
          },
          "lateExitStop": {
            ".validate": "newData.isString()"
          },
          "lateExitLatLng": {
            ".validate": "newData.hasChildren(['lat', 'lng'])"
          }
        }
      }
    },
    "conductorLocations": {
      ".indexOn": ["timestamp", "conductorId"],
      "$conductorId": {
        ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() == 'Rider' || auth.uid == $conductorId || root.child('users').child($conductorId).child('ownerId').val() == auth.uid)",
        ".write": "auth != null && auth.uid == $conductorId"
      }
    },
    "messages": {
      "$requestId": {
        ".read": "auth != null && ((root.child('requests').child($requestId).child('status').val() == 'Accepted' || root.child('requests').child($requestId).child('status').val() == 'Completed') && (root.child('requests').child($requestId).child('riderId').val() == auth.uid || root.child('requests').child($requestId).child('conductorId').val() == auth.uid))",
        ".write": "false",
        "messages": {
          "$messageId": {
            ".write": "auth != null && (root.child('requests').child($requestId).child('status').val() == 'Accepted' || root.child('requests').child($requestId).child('status').val() == 'Completed') && (root.child('requests').child($requestId).child('riderId').val() == auth.uid || root.child('requests').child($requestId).child('conductorId').val() == auth.uid) && newData.child('senderId').val() == auth.uid"
          }
        }
      }
    },
    "conductorRatings": {
      ".indexOn": ["conductorId"],
      "$conductorId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "busRatings": {
      ".indexOn": ["busId"],
      "$busId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
 */
}