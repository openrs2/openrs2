var buildRegex = new RegExp('>([0-9]+)(?:[.]([0-9]+))?<');

function customSort(name, order, data) {
	order = order === 'asc' ? 1 : -1;

	data.sort(function (a, b) {
		a = a[name];
		b = b[name];

		if (!a) {
			return 1;
		} else if (!b) {
			return -1;
		}

		if (name === 'builds') {
			return buildSort(a, b) * order;
		} else {
			if (a < b) {
				return -order;
			} else if (a === b) {
				return 0;
			} else {
				return order;
			}
		}
	});
}

function buildSort(a, b) {
	a = buildRegex.exec(a);
	b = buildRegex.exec(b);

	var aMajor = parseInt(a[1]);
	var bMajor = parseInt(b[1]);
	if (aMajor !== bMajor) {
		return aMajor - bMajor;
	}

	var aMinor = a[2] ? parseInt(a[2]) : 0;
	var bMinor = b[2] ? parseInt(b[2]) : 0;
	return aMinor - bMinor;
}

$(function () {
	$('#paginated-table').on('post-body.bs.table', function () {
		$(this).fadeIn();
	});
})
