+String {
	// convert string in phex style to an array
	// of values mapped according to spec and
	// with spaces replaced with \rests
	phexToPatternList { |spec|
		var getSubPhex;

		var a = [], i = 0;
		
		// make sure we have a spec of some sort to map characters with
		spec = (spec ?? { [0, 16] }).asSpec;

		getSubPhex = {
			var subPhex = "";
			
			// shift i along until we find a . or the end of the Phex
			while({ (this.at(i) != $\.) && (i < this.size) }) {
				subPhex = subPhex ++ this.at(i);
				i = i + 1;
			};

			subPhex;
		};

		// step through each of the tokens
		while({ i < this.size }) {
			var item, token;

			token = this.at(i);

			// handle special characters
			item = switch(token,
				// space is a rest
				$ , { \rest }, 
				
				// ? is random in full range of spec
				$\?, { 
					i = i + 1;
					Phuf(getSubPhex.(), spec, 1); 
				},
				
				// *n - repeats the subphex between it and the next .
				// n times
				$\*, { 
					var repeats;
					
					// get the number of repetions as param for *
					repeats = this.at(i + 1).digit.clip(0, 15);

					// skip over repeats param 
					i = i + 2;

					// make patterns from our subphex
					Phex(getSubPhex.(), spec, repeats);
				},
			);

			// if character hasn't been mapped yet map it from hex using spec
			item = item ?? { spec.map(token.digit.clip(0, 15) / 15) };

			a = a.add(item);

			i = i + 1;
		};

		^a;
	}
}
