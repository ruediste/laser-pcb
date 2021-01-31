package com.github.ruediste.laserPcb.profile;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.RepositoryBase;

@Service
@Scope("singleton")
public class ProfileRepository extends RepositoryBase<Profile> {

}
