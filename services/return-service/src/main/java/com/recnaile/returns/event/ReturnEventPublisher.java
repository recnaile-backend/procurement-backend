package com.recnaile.returns.event;

import com.recnaile.returns.model.entity.ReturnRequest;

public interface ReturnEventPublisher {
    void publish(ReturnRequest returnRequest);
}
