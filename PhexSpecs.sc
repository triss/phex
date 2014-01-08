PhexSpecs {
	*add {
		// specs for adsr
		Spec.add(\att, [0.001, 32, \exp, 0, 0.1]);
		Spec.add(\dec, [0.001, 32, \exp, 0, 0.3]);
		Spec.add(\sus, [0, 1, \lin, 0, 0.5]);
		Spec.add(\rel, [0.001, 32, \exp, 0, 1]);

		// filter specs
		Spec.add(\cutoff, [30, 16000, \exp, 0, 16000, 'hz']);
		Spec.add(\rez, [1, 0.001, \exp, 0, 0.5]);
		Spec.add(\moogRez, [0.001, 4, \exp, 0, 3]);
		Spec.add(\dfmRez, [0.001, 1.1, \exp, 0, 0.5]);

		// vibrato and tremelo
		Spec.add(\vibrato, [0, 1]);
		Spec.add(\vibratoRate, [0, 10]);
		Spec.add(\tremelo, [0, 1]);
		Spec.add(\tremeloRate, [0, 10]);

		// startPos for samples
		Spec.add(\startPos, [0, 15/16]);

		// legato
		Spec.add(\legato, [0.1, 16, \exp]);
	}
}
