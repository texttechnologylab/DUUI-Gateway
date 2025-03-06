<script>
	import FunctionalTable from '$lib/svelte/components/FunctionalTable.svelte'
	import JsonDropdownInput from '$lib/svelte/components/Input/JsonDropdownInput.svelte'
	import {faDeleteLeft, faPlus, faSave} from '@fortawesome/free-solid-svg-icons'
	import Text from '$lib/svelte/components/Input/TextInput.svelte'
	import Fa from 'svelte-fa'
	import Dropdown from "$lib/svelte/components/Input/Dropdown.svelte";

	export let labels = ["a", "b", "c", "d", "e", "f", "g"]
	export let users = ["a", "b", "c", "d", "e", "f", "g"]

	export let rows = {
		row1: { label: 'g1', driver: "", scope: "public", labels: []},
		row2: { label: 'g2', driver: "", scope: "public", labels: ["a", "b"]}
	};

	export let saveCallback = (key, row) => {
		return () => {
			console.log(key, row)
		}
	}
	export let insertCallback= (key, row) => {
		return () => {
			console.log(key, row)
		}
	}
	export let deleteCallback= (key, row) => {
		return () => {
			console.log(key, row)
		}
	}

	let newGroupName

	const columns = { label: "Label"};


	let functionalColumns = [
		{
			label: 'Driver',
			render: (key, row) => Dropdown,
			props: (key, row) => ({
				name: key,
				style: "button-menu px-4 py-2 !self-stretch h-full",
				border: "!border-y !border-x border-color",
				options: ["DUUIDockerDriver", "DUUIKubernetesDriver", "DUUISwarmDriver"],
				value: row["driver"]
			})
		},
		{
			label: 'Scope',
			render: (key, row) => Dropdown,
			props: (key, row) => ({
				name: key,
				style: "button-menu px-4 py-2 !self-stretch h-full",
				border: "!border-y !border-x border-color",
				options: ["public", "group"],
				value: row["scope"]
			})
		},
		{
			label: 'Save',
			render: (key, row) => ({
				type: 'button',
				props: {
					class: "!justify-center !border-none !rounded-none",
					onClick: saveCallback(key, row),
					icon: faSave
				}
			})
		},
		{
			label: 'Delete',
			render: (key, row) => ({
				type: 'button',
				props: {
					class: "!justify-center !border-none !rounded-none",
					onClick: deleteCallback(key, row),
					icon: faDeleteLeft
				}
			})
		}
	];
</script>

<FunctionalTable data={ rows } columns={ columns } { functionalColumns } />
<div style="display: flex; align-items: center; justify-content: flex-end;">
	<Text
		label=""
		style="self-center"
		name="newLabel"
		bind:value={newGroupName}
	/>
	<button
		class="button-neutral !justify-center !border-none !rounded-none"
		on:click={() => insertCallback(newGroupName, rows)}>
		<Fa icon={faPlus} />
	</button>
</div>