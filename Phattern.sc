Phattern {
	var synth;
	
	var pProxies;

	var length;

	var <channel;

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
		this.updatePattern(\out, channel.bus); 
		this.updatePattern(\group, channel.groups.sources); 
	}

	synth_ { |syn|
		synth = syn;
		this.changed(\synth);
	}

	updatePattern { |param pattern|
		// if "pattern" is a string convert it to a Phex
		if(pattern.isString) { 
			var spec;

			if(specs.includesKey(param)) {
				// if a spec has been set for this param on this Phattern use that
				spec = specs[param];
			} {
				// otherwise look in synth metadata
				if(SynthDescLib.global[synth].notNil) {
					var synthDesc = SynthDescLib.global[synth];
					if(synthDesc.metadata.notNil) {	
						if(synthDesc.metadata.specs.notNil) {
							if(synthDesc.metadata[specs].notNil) {
								spec = synthDesc.metadata[specs][param];
							};			
						};
					};
				};
			};

			// if nonoe of tha above found a spec look one up in spec library
			spec = (spec ?? param).asSpec;

			pattern = Phex(pattern, spec);
		};

		// update pattern proxies
		if(pProxies[param].isNil) {
			pProxies[param] = PatternProxy(pattern);
		} {
			pProxies[param].source = pattern;
		};

		// update ouputted proxy
		outProxy.source = Pbind(
			\instrument, synth, *pProxies.getPairs
		);
	}

	// if we don't understand the message it's a pattern being updated or 
	// recalled.
	doesNotUnderstand { |selector val| 
		if(selector.isSetter) {
			// if this is a setter upfate the pattern
			this.updatePattern(selector.asGetter, val);
		} {
			// if it's a getter return the pattern that's already set
			^pProxies[selector];
		};
	}

	// here we use object's depemdacy mechanism to recive update from parent
	// or environment
	update { |changer param pattern|
		[changer, param, pattern].postln;
		this.updatePattern(param, pattern)
	}
}

