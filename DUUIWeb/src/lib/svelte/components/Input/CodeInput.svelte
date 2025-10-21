<script lang="ts">
	import { createEventDispatcher, onDestroy, onMount } from 'svelte';

	export let error = false;      // turn inputs red on error
	export let minutes = 10;       // start timer in minutes

	const dispatch = createEventDispatcher<{ verify: { code: string } }>();

	const LEN = 6;
	let code: string[] = Array(LEN).fill('');
	let inputs: HTMLInputElement[] = [];

	function focus(i: number) {
		inputs[i]?.focus();
		inputs[i]?.select();
	}

	onMount(() => focus(0));

	function setFromString(s: string) {
		const digits = s.replace(/\D/g, '').slice(0, LEN).split('');
		code = Array(LEN).fill('').map((_, i) => digits[i] ?? '');
		const filled = code.findIndex((c) => c === '');
		if (filled === -1) submit();
		else focus(filled);
	}

	function onInput(i: number, e: Event) {
		const v = (e.target as HTMLInputElement).value.replace(/\D/g, '');
		if (v.length > 1) { setFromString(v); return; }
		code[i] = v;
		if (v && i < LEN - 1) focus(i + 1);
	}

	function onKeyDown(i: number, e: KeyboardEvent) {
		const t = e.target as HTMLInputElement;
		if (e.key === 'Backspace') {
			if (t.value === '' && i > 0) { code[i - 1] = ''; focus(i - 1); e.preventDefault(); }
			else { code[i] = ''; }
		} else if (e.key === 'ArrowLeft' && i > 0) { focus(i - 1); e.preventDefault(); }
		else if (e.key === 'ArrowRight' && i < LEN - 1) { focus(i + 1); e.preventDefault(); }
		else if (e.key === 'Enter') submit();
	}

	function onPaste(e: ClipboardEvent) {
		const text = e.clipboardData?.getData('text') ?? '';
		if (/\d/.test(text)) { e.preventDefault(); setFromString(text); }
	}

	function submit() {
		const val = code.join('');
		if (val.length === LEN) dispatch('verify', { code: val });
	}

	// --- countdown ---
	let secondsLeft = Math.max(0, Math.floor(minutes * 60));
	let interval: number | undefined;

	function formatMMSS(s: number) {
		const m = Math.floor(s / 60).toString().padStart(2, '0');
		const sec = (s % 60).toString().padStart(2, '0');
		return `${m}:${sec}`;
	}

	onMount(() => {
		interval = window.setInterval(() => {
			if (secondsLeft > 0) secondsLeft -= 1;
			else clearInterval(interval);
		}, 1000);
	});
	onDestroy(() => clearInterval(interval));
</script>

<!-- Container is relative so the timer can sit bottom-left -->
<div class="relative flex flex-col gap-4" on:paste={onPaste}>
	<label class="text-sm opacity-70">Verification code</label>

	<div class="flex gap-2">
		{#each Array(LEN) as _, i}
			<input
				bind:this={inputs[i]}
				inputmode="numeric"
				autocomplete="one-time-code"
				maxlength="1"
				class={`w-12 h-12 text-center text-xl rounded-xl border bg-surface-100/60
						focus:outline-none focus:ring-2
						${error
							? 'border-error-500 focus:ring-error-500'
							: 'border-surface-400/40 focus:ring-primary-500'}`}
				value={code[i]}
				on:input={(e) => onInput(i, e)}
				on:keydown={(e) => onKeyDown(i, e)}
			/>
		{/each}
	</div>

	<button
		class="btn btn-primary"
		aria-label="Verify code"
		on:click={submit}
		disabled={code.join('').length !== LEN}
	>
		Enter
	</button>

	<!-- timer -->
	<div
		class="absolute left-0 -bottom-6 text-xs text-surface-500 select-none"
		aria-live="polite"
	>
		{formatMMSS(secondsLeft)}
	</div>
</div>

<style>
    .btn {
        @apply inline-flex items-center justify-center px-4 py-2 rounded-xl border border-surface-400/40 bg-primary-600 text-white disabled:opacity-40;
    }
</style>
