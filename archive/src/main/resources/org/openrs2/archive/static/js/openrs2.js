var buildRegex = new RegExp('>([0-9]+)(?:[.]([0-9]+))?<');

function buildSort(a, b) {
	a = buildRegex.exec(a);
	b = buildRegex.exec(b);
	if (!a) {
		return -1;
	} else if (!b) {
		return 1;
	}

	var aMajor = parseInt(a[1]);
	var bMajor = parseInt(b[1]);
	if (aMajor !== bMajor) {
		return aMajor - bMajor;
	}

	var aMinor = a[2] ? parseInt(a[2]) : 0;
	var bMinor = b[2] ? parseInt(b[2]) : 0;
	return aMinor - bMinor;
}
