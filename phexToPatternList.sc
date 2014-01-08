+String {
	// convert string in phex style to an array
	// of values mapped according to spec and
	// with spaces replaced with \rests
	phexToPatternList { |spec|
		// TODO - make dictionary of "command" characters - way neater
		// can outer phex/subphex thing be made made with only one loop? - do 
		// I need to getSubPhex?
	
		var getSubPhex;

		var a = [], i = 0;
		
		// make sure we have a spec of some sort to map characters with
		spec = (spec ?? { [0, 16] }).asSpec;

		// function to handle recursively parsing subphexs
		getSubPhex = {
			var subPhex = "", nSubPhexs = 1;
			
			i = i + 1;

			// shift i along until we find a . or the end of the Phex
			while({ (nSubPhexs > 0) && (i < this.size) }) {
				switch(this.at(i),
					$\., { nSubPhexs = nSubPhexs - 1 },
					$\?, { nSubPhexs = nSubPhexs + 1 },
					$\*, { nSubPhexs = nSubPhexs + 1 }
				);

				if(nSubPhexs > 0) { 
					subPhex = subPhex ++ this.at(i);
					i = i + 1;
				};
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
					Phuf(getSubPhex.(), spec, 1); 
				},
				
				// *n - repeats the subphex between it and the next .
				// n times
				$\*, { 
					var repeats;

					// shift to repeats param 
					i = i + 1;
					
					// get the number of repetions as param for *
					repeats = this.at(i).digit.clip(0, 15);

					// make patterns from our subphex
					Phex(getSubPhex.(), spec, repeats);
				},
			);

			item = item ?? { spec.map(token.digit.clip(0, 15) / 15) };

			a = a.add(item);

			i = i + 1;
		};

		^a;
	}
}
