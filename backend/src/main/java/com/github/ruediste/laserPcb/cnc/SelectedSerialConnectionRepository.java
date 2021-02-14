package com.github.ruediste.laserPcb.cnc;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.SingletonRepositoryBase;

@Service
@Scope("singleton")
public class SelectedSerialConnectionRepository extends SingletonRepositoryBase<String> {

}
