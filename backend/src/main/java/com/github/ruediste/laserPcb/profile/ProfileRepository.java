package com.github.ruediste.laserPcb.profile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.RepositoryBase;

@Service
@Scope("singleton")
public class ProfileRepository extends RepositoryBase<Profile> {
	@Autowired
	CurrentProfileIdRepository currentIdRepo;

	public Profile getCurrent() {
		CurrentProfileId currentProfileId = currentIdRepo.get();
		if (currentProfileId == null)
			return null;
		return get(currentProfileId.id);
	}

}
