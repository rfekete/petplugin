package ai.aitia.petplugin.launch;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;

import ai.aitia.petplugin.natures.PetNature;

import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;

public class PetLaunchTester extends PropertyTester{

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) 
	{

		assert (receiver != null);
		IResource resource = AdapterUtilities.getAdapter(receiver, IResource.class);

		if (resource == null) {
			// Unexpected case; we were asked to test against something that's
			// not a resource.
			return false;
		}

		// Resolve to the actual resource (if it is linked)
		resource = ResourceUtils.resolveTargetResource(resource);

		return (PetNature.isPetProject(resource.getProject()));
	}

}
