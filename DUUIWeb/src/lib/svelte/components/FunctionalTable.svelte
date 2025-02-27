<script>
	import Fa from 'svelte-fa'

	export let data = {}; // Object containing row data
	export let columns = {}; // Object defining column labels: { columnKey: 'Column Label' }
	export let functionalColumns = []; // Functional column definitions: { label: 'Action', render: (rowKey, row) => {...} }
</script>

<div class="section-wrapper p-8 space-y-4 w-full">
	<h3 class="h3">Table</h3>
	<div class="bordered-soft rounded-md overflow-hidden max-w-full">
		<div class="overflow-x-auto">
			<div class="grid w-full bg-surface-100-800-token gap-0.5"
					 style={`grid-template-columns: repeat(${Object.keys(columns).length + functionalColumns.length}, 1fr);`}>
				{#each Object.entries(columns) as [key, label]}
					<p class="p-4 break-words text-center">{label}</p>
				{/each}
				{#each functionalColumns as fColumn}
					<p class="p-4 text-center">{fColumn.label}</p>
				{/each}
			</div>

			{#each Object.entries(data) as [key, row]}
				<div class="grid w-full border-t border-color gap-0.5 p-1"
						 style={`grid-template-columns: repeat(${Object.keys(columns).length + functionalColumns.length}, 1fr);`}>

					{#each Object.keys(columns) as columnKey}
						{#if row[columnKey] !== undefined} <!-- Ensures undefined values don't break it -->
							<p class="p-4 break-words">{row[columnKey]}</p>
						{/if}
					{/each}

					{#each functionalColumns as fColumn}
						{#await Promise.resolve(fColumn.render(key, row)) then result}
							{#if result.type === 'button'}
								<button class={result.props.class} on:click={result.props.onClick}>
									<Fa icon={result.props.icon} />
								</button>
							{:else}
								<svelte:component this={result} {...fColumn.props(key, row)} />
							{/if}
						{/await}
					{/each}
				</div>
			{/each}

		</div>
	</div>
</div>
