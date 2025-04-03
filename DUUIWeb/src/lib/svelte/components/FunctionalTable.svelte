<script>
	import Fa from 'svelte-fa';

	// Props:
	// - data: an object of rows keyed by unique IDs.
	// - columns: an object mapping each column key to its header label.
	// - functionalColumns: an array of definitions for extra columns (with custom render functions).
	// - title: an optional title for the table.
	export let data = {};
	export let columns = {};
	export let functionalColumns = [];
	export let title = "Table";

	// Filtering, sorting, and pagination state:
	let filterQuery = "";
	let sortColumn = "";
	let sortDirection = 1; // 1 for ascending, -1 for descending
	let currentPage = 1;
	let rowsPerPage = 10;

	// Convert the data object to an array of rows with an 'id' property.
	$: rowsArray = Object.entries(data).map(([id, row]) => ({ id, ...row }));

	// Filtering: Return rows where any column's value contains the filter query (case-insensitive)
	$: filteredRows = rowsArray.filter(row => {
		if (!filterQuery) return true;
		return Object.keys(columns).some(col => {
			const value = row[col];
			if (value === undefined || value === null) return false;
			return value.toString().toLowerCase().includes(filterQuery.toLowerCase());
		});
	});

	// Sorting: Sort filtered rows based on sortColumn and sortDirection
	$: sortedRows = [...filteredRows].sort((a, b) => {
		if (!sortColumn) return 0;
		let aValue = a[sortColumn];
		let bValue = b[sortColumn];
		if (aValue == null) return 1;
		if (bValue == null) return -1;
		if (typeof aValue === "number" && typeof bValue === "number") {
			return (aValue - bValue) * sortDirection;
		}
		return aValue.toString().localeCompare(bValue.toString()) * sortDirection;
	});

	// Pagination: Calculate total pages and slice the sorted rows for the current page.
	$: totalPages = Math.ceil(sortedRows.length / rowsPerPage);
	$: paginatedRows = sortedRows.slice((currentPage - 1) * rowsPerPage, currentPage * rowsPerPage);

	// Toggle sorting on a column. Resets pagination to page 1.
	function toggleSort(col) {
		if (sortColumn === col) {
			sortDirection = -sortDirection;
		} else {
			sortColumn = col;
			sortDirection = 1;
		}
		currentPage = 1;
	}

	// Pagination control functions.
	function goToPage(page) {
		if (page >= 1 && page <= totalPages) {
			currentPage = page;
		}
	}
	function nextPage() {
		if (currentPage < totalPages) currentPage += 1;
	}
	function prevPage() {
		if (currentPage > 1) currentPage -= 1;
	}
</script>

<div class="section-wrapper p-8 space-y-4 w-full">
	{#if title}
		<h3 class="h3">{title}</h3>
	{/if}

	<!-- Filter Input -->
	<div class="mb-4">
		<input
				type="text"
				placeholder="Filter table..."
				bind:value={filterQuery}
				class="border p-2 rounded w-full"
		/>
	</div>

	<div class="bordered-soft rounded-md overflow-hidden max-w-full">
		<div class="overflow-x-auto">
			<!-- Table Header -->
			<div
					class="grid w-full bg-surface-100-800-token gap-0.5"
					style={`grid-template-columns: repeat(${Object.keys(columns).length + functionalColumns.length}, 1fr);`}>
				{#each Object.entries(columns) as [colKey, label]}
					<p
							class="p-4 break-words text-center cursor-pointer select-none"
							on:click={() => toggleSort(colKey)}>
						{label}
						{#if sortColumn === colKey}
							<span>{sortDirection === 1 ? ' ▲' : ' ▼'}</span>
						{/if}
					</p>
				{/each}
				{#each functionalColumns as fColumn}
					<p class="p-4 text-center">{fColumn.label}</p>
				{/each}
			</div>

			<!-- Table Body -->
			{#each paginatedRows as row}
				<div
						class="grid w-full border-t border-color gap-0.5 p-1"
						style={`grid-template-columns: repeat(${Object.keys(columns).length + functionalColumns.length}, 1fr);`}>
					{#each Object.keys(columns) as colKey}
						<p class="p-4 break-words text-center">
							{row[colKey] !== undefined ? row[colKey] : ''}
						</p>
					{/each}

					{#each functionalColumns as fColumn}
						{#await Promise.resolve(fColumn.render(row.id, row)) then result}
							{#if result && result.type === 'button'}
								<button class={result.props.class} on:click={result.props.onClick}>
									<Fa icon={result.props.icon} />
								</button>
							{:else if result}
								<svelte:component
										this={result}
										{...(fColumn.props ? fColumn.props(row.id, row) : {})} />
							{/if}
						{:catch error}
							<span>Error: {error}</span>
						{/await}
					{/each}
				</div>
			{/each}
		</div>
	</div>

	<!-- Pagination Controls -->
	<div class="flex items-center justify-center space-x-4 mt-4">
		<button on:click={prevPage} class="p-2 bg-gray-300 rounded disabled:opacity-50" disabled={currentPage === 1}>
			Previous
		</button>
		<span>Page {currentPage} of {totalPages}</span>
		<button on:click={nextPage} class="p-2 bg-gray-300 rounded disabled:opacity-50" disabled={currentPage === totalPages}>
			Next
		</button>
	</div>
</div>
