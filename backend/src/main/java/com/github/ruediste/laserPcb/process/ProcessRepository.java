package com.github.ruediste.laserPcb.process;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.SingletonRepositoryBase;

@Service
@Scope("singleton")
public class ProcessRepository extends SingletonRepositoryBase<Process> {

}
