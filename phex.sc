Phattern {
	var synth;
	
	var pProxies;

	var length;

	var channel;

	var <outProxy;

	var <specs;

	*new { |synth|
		^super.new.init(synth);
	}

	init { |syn|
		this.synth_(syn);

		pProxies = IdentityDictionary();
		specs = IdentityDictionary();

		outProxy = EventPatternProxy();

		// create a mixer channel for audio and fx to live on
		channel = PhexChannel();

		// set patterns to route audio to correct bits of mixer
		this.updatePatterns(\out, channel.bus); 
		this.updatePatterns(\group, channel.group.sources); 
	}

	synth_ { |syn|
		synth = syn;
		this.changed(\synth);
	}

	updatePatterns { |selector val|
		if(val.isString) { 
			// do we have a spec for selector?
			var spec = specs[selector] ?? { selector.asSpec };

			val = Phex(val, spec);
		};

		if(pProxies[selector].isNil) {
			pProxies[selector] = PatternProxy(val);
		} {
			pProxies[selector].source = val;
		};

		outProxy.source = Pbind(
			\instrument, synth, *pProxies.getPairs
		);
	}

	doesNotUnderstand { |selector val| 
		if(selector.isSetter) {
			this.updatePatterns(selector.asGetter, val);
		} {
			^pProxies[selector];
		};
	}

	update { |changer what changed|
		switch(what,
			\scale, { this.updatePatterns(\scale, changed) },
			\root, { this.updatePatterns(\root, changed) }
		);
	}
}

