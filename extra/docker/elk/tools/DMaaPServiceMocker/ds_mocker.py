#!/usr/bin/env python3
import os
import json
import copy
import random
import requests
import uuid
import time
from datetime import datetime

def luck(n=2):
    """ gives 1 chance out of n (default: 2) to return True """
    assert n > 1
    return bool(random.randint(0, n-1))
def now_dmaap_timestamp():
    return str(datetime.now().timestamp()).replace(".","")[:13]
def now_notification_time():
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f+00:00")

CONTROL_LOOP_NAMES = [
    'ClosedLoop-vUSP-SIG-d925ed73-8231-4d02-9545-db4e101f88f8',
    'ClosedLoop-vUSP-SIG-37b1c91e-fd6b-4abd-af15-771902d5fdb1',
    'ClosedLoop-vUSP-SIG-c2597657-7113-4efb-a1f9-397a7a24c3e1',
    'ClosedLoop-vUSP-SIG-a11318ba-4c61-46b8-a9da-bd40bec11d45',
    'ClosedLoop-vUSP-SIG-5321c558-2254-4efb-ac24-99dd54edc94f',
]

TEMPLATES = {
    'event_abated'                   :'event_abated.json',
    'event_onset'                    :'event_onset.json',
    'notification_active'            :'notification_active.json',
    'notification_final_failed'      :'notification_final_failed.json',
    'notification_final_open'        :'notification_final_open.json',
    'notification_final_success'     :'notification_final_success.json',
    'notification_operation_failure' :'notification_operation_failure.json',
    'notification_operation'         :'notification_operation.json',
    'notification_operation_success' :'notification_operation_success.json',
    'notification_rejected_disabled' :'notification_rejected_disabled.json',
    'notification_rejected_missing'  :'notification_rejected_missing.json',
}

for key in TEMPLATES:
    with open(TEMPLATES[key]) as f:
        content = f.read()
    TEMPLATES[key] = json.loads(content)


class DMaaPMessage(dict):

    dmaap_host_url = "http://dmaap.host.url:9200/"
    dmaap_username = None
    dmaap_password = None

    @classmethod
    def from_template(cls, tmpl, **kwargs):
        obj = cls()
        obj.update(copy.deepcopy(TEMPLATES[tmpl]))
        for keys,value in kwargs.items():
            current_node = obj
            keys = keys.split(".")
            key = keys[0]
            for i in range(len(keys) - 1):
                current_node = current_node[keys[i]]
                key = keys[i]
            current_node[key] = value
        return obj

    def publish(self, topic):
        url = "%s/events/%s" % (self.dmaap_host_url, topic)
        auth = None
        if self.dmaap_username and self.dmaap_password:
            auth = (self.dmaap_username, self.dmaap_password)
        response = requests.post(url, data=json.dumps(self), auth=auth)
        return response.status_code

class Event(DMaaPMessage):

    topic = "DCAE-CL-EVENT"

    @staticmethod
    def abated(**kwargs):
        return Event.from_template('event_abated', **kwargs)

    @staticmethod
    def onset(**kwargs):
        return Event.from_template('event_onset', **kwargs)

    def publish(self):
        return super().publish(self.topic)


class Notification(DMaaPMessage):

    topic = "POLICY-CL-MGT"

    @classmethod
    def from_template(cls, tmpl, **kwargs):
        kwargs['notificationTime'] = now_notification_time()
        return super().from_template(tmpl, **kwargs)

    @staticmethod
    def active(**kwargs):
        return Notification.from_template('notification_active', **kwargs)

    @staticmethod
    def final(**kwargs):
        class FinalNotification(Notification):
            @staticmethod
            def success(**kwargs):
                return FinalNotification.from_template('notification_final_success', **kwargs)
            @staticmethod
            def failed(**kwargs):
                return FinalNotification.from_template('notification_final_failed', **kwargs)
            @staticmethod
            def open(**kwargs):
                return FinalNotification.from_template('notification_final_open', **kwargs)
        return FinalNotification

    @staticmethod
    def operation(**kwargs):
        class OperationNotification(Notification):
            @staticmethod
            def success(**kwargs):
                return OperationNotification.from_template('notification_operation_success', **kwargs)
            @staticmethod
            def failure(**kwargs):
                return OperationNotification.from_template('notification_operation_failure', **kwargs)
        return OperationNotification.from_template('notification_operation', **kwargs)

    @staticmethod
    def rejected(**kwargs):
        class RejectedNotification(Notification):
            @staticmethod
            def disabled(**kwargs):
                return RejectedNotification.from_template('notification_rejected_disabled', **kwargs)
            @staticmethod
            def missing_fields(**kwargs):
                return RejectedNotification.from_template('notification_rejected_missing', **kwargs)

        return RejectedNotification

    def publish(self):
        return super().publish(self.topic)



class CLStatus(object):

    def __init__(self, dmaap_url=None,
                 missing=None, disabled=None, op_failure=None):
        self._stopped = False
        def maybe(thing):
            if thing is None:
                thing = not luck(10)
            return thing
        self._missing = maybe(missing)
        self._disabled = maybe(disabled)
        self._op_failure = maybe(op_failure)
        self._config = dict(
            requestID=str(uuid.uuid4()),
            closedLoopControlName=CONTROL_LOOP_NAMES[random.randint(0, len(CONTROL_LOOP_NAMES) - 1)]
        )

    def __iter__(self):
        return next(self)

    def __next__(self):
        if self._stopped:
            raise StopIteration()
        config = self._config
        config.update(dict(closedLoopAlarmStart=now_dmaap_timestamp()))
        yield Event.onset(**config)
        if self._missing:
            self._stopped = True
            yield Notification.rejected().missing_fields(**config)
            raise StopIteration()
        elif self._disabled:
            self._stopped = True
            yield Notification.rejected().disabled(**config)
            raise StopIteration()

        yield Notification.active(**config)
        yield Notification.operation(**config)

        config['closedLoopAlarmEnd'] = now_dmaap_timestamp()
        if self._op_failure:
            yield Notification.operation().failure(**config)
            self._stopped = True
            yield Notification.final().failed(**config)
        else:
            yield Notification.operation().success(**config)
            yield Event.abated(**config)
            self._stopped = True
            yield Notification.final().success(**config)
        raise StopIteration()

DMaaPMessage.dmaap_host_url = "http://uebsb93kcdc.it.att.com:3904"
Event.topic = "APPC-TEST-AMDOCS1-1607-E2E"
Notification.topic = "APPC-TEST-AMDOCS1-1607-IST"
# Request.topic = "APPC-TEST-AMDOCS1-1607-RY303T"

test_datas = [CLStatus(missing=False, disabled=False, op_failure=False) for i in range(45)]  \
             + [CLStatus(missing=True, disabled=False, op_failure=False) for i in range(5)]  \
             + [CLStatus(missing=False, disabled=True, op_failure=False) for i in range(6)]  \
             + [CLStatus(missing=False, disabled=False, op_failure=True) for i in range(7)]
random.shuffle(test_datas)
random.shuffle(test_datas)

for current_i, status in enumerate(test_datas):
    time.sleep(random.randint(0,6))
    for s in status:
        # print(s)
        status_code = s.publish()
        if status_code != 200:
            print("Error when publishing : status_code={}".format(status_code))
            exit(1)
        time.sleep(random.randint(0,3))
    print("%03d,missing:%5s,disabled:%5s,op_failure:%5s - %s" % (current_i, status._missing, status._disabled, status._op_failure, status._config))
