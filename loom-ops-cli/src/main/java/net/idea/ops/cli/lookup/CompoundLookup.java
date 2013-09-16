package net.idea.ops.cli.lookup;

import java.net.MalformedURLException;
import java.net.URL;

import net.idea.opentox.cli.structure.Compound;

public class CompoundLookup extends Lookup<Compound> {


	/**
	 * 
	 */
	private static final long serialVersionUID = -8347596431836423888L;

	@Override
	protected Compound create(String key) throws MalformedURLException {
		return new Compound(new URL(key));
	}

}
