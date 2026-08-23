(async function autoRetryOCI() {
  const WAIT_BETWEEN_ATTEMPTS = 60000;
  const WAIT_AFTER_AD_CYCLE   = 300000;
  const WAIT_ON_NO_CREATE_BTN = 20000;
  window.__ociRetryStop = false;

  const ADs = [
    "yaxd:EU-FRANKFURT-1-AD-1",
    "yaxd:EU-FRANKFURT-1-AD-2",
    "yaxd:EU-FRANKFURT-1-AD-3"
  ];
  let adIndex = 0;
  let attempt = 0;
  let fullCycles = 0;

  const sleep = (ms) => new Promise(r => setTimeout(r, ms));

  const fmtTime = (ms) => {
    const s = Math.round(ms / 1000);
    if (s < 60) return `${s}s`;
    return `${Math.floor(s / 60)}min ${s % 60}s`;
  };

  // ✅ Fixed: check both offsetParent AND getBoundingClientRect (more reliable)
  const isVisible = (el) => {
    if (!el) return false;
    if (el.offsetParent !== null) return true;
    const rect = el.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0;
  };

  const findButtonByLabel = (label) =>
    [...document.querySelectorAll('button[aria-label]')]
      .find(b => b.getAttribute('aria-label') === label && isVisible(b) && !b.disabled);

  // ✅ New: retry search for a button over a few seconds instead of giving up instantly
  const waitForButton = async (label, timeout = 8000, interval = 400) => {
    const start = Date.now();
    while (Date.now() - start < timeout) {
      const btn = findButtonByLabel(label);
      if (btn) return btn;
      await sleep(interval);
    }
    return null;
  };

  const findEditButtonForSection = (sectionText) => {
    const headings = [...document.querySelectorAll('h1,h2,h3,h4,div,span')]
      .filter(el => el.textContent.trim() === sectionText);
    for (const h of headings) {
      let container = h.closest('div');
      for (let i = 0; i < 6 && container; i++) {
        const btn = container.querySelector('button[aria-label="Edit"]');
        if (btn && isVisible(btn)) return btn;
        container = container.parentElement;
      }
    }
    return null;
  };

  const waitFor = async (checkFn, timeout = 15000, interval = 300) => {
    const start = Date.now();
    while (Date.now() - start < timeout) {
      const result = checkFn();
      if (result) return result;
      await sleep(interval);
    }
    return null;
  };

  const selectAD = async (adValue) => {
    const radio = await waitFor(() => document.querySelector(`input[type="radio"][value="${adValue}"]`));
    if (!radio) {
      console.log("❌ Could not find radio button for AD:", adValue);
      return false;
    }
    radio.scrollIntoView({ block: "center" });
    await sleep(300);
    radio.click();
    const label = radio.closest('.oj-oci-base-selection-card');
    if (label) label.click();
    console.log("✅ AD selected:", adValue);
    return true;
  };

  const debugListButtons = () => {
    const labels = [...document.querySelectorAll('button[aria-label]')]
      .map(b => `${b.getAttribute('aria-label')}(visible:${isVisible(b)},disabled:${b.disabled})`);
    console.log("🔍 Buttons found on page:", labels.length ? labels.join(', ') : 'NONE');
  };

  const clickNextUntilCreate = async (maxSteps = 8) => {
    for (let i = 0; i < maxSteps; i++) {
      const createBtn = await waitForButton("Create", 3000);
      if (createBtn) return createBtn;

      const nextBtn = await waitForButton("Next", 3000);
      if (nextBtn) {
        nextBtn.scrollIntoView({ block: "center" });
        await sleep(300);
        nextBtn.click();
        console.log("➡️ Clicked Next, step:", i + 1);
        await sleep(1800);
      } else {
        console.log("⚠️ Neither Next nor Create found, step:", i + 1);
        debugListButtons();
        await sleep(1500);
      }
    }
    return findButtonByLabel("Create");
  };

  const checkError = () => {
    const bodyText = document.body.innerText;
    return bodyText.includes("Out of capacity") || bodyText.includes("API Error");
  };

  console.log(`🎬 Script started. Wait between attempts: ${fmtTime(WAIT_BETWEEN_ATTEMPTS)} | Wait after full AD cycle: ${fmtTime(WAIT_AFTER_AD_CYCLE)}`);

  while (!window.__ociRetryStop) {
    attempt++;
    console.log(`\n🔄 Attempt #${attempt} — AD: ${ADs[adIndex]} — ${new Date().toLocaleTimeString()}`);

    // ✅ Give the page a moment to settle before searching
    await sleep(1500);

    const editBtn = findEditButtonForSection("Basic information");
    if (editBtn) {
      editBtn.click();
      await sleep(1500);
      await selectAD(ADs[adIndex]);
      await sleep(800);
    } else {
      console.log("⚠️ Could not find Edit button for Basic information, trying Create directly");
    }

    let createBtn = await clickNextUntilCreate();

    if (!createBtn) {
      console.log(`❌ Could not find Create button. Waiting ${fmtTime(WAIT_ON_NO_CREATE_BTN)}...`);
      debugListButtons();
      await sleep(WAIT_ON_NO_CREATE_BTN);
      continue;
    }

    createBtn.scrollIntoView({ block: "center" });
    await sleep(300);
    createBtn.click();
    console.log("🚀 Clicked Create, waiting for result...");
    await sleep(6000);

    if (checkError()) {
      console.log(`❌ Still "Out of capacity" with ${ADs[adIndex]}.`);

      const wasLastAD = adIndex === ADs.length - 1;
      adIndex = (adIndex + 1) % ADs.length;

      if (wasLastAD) {
        fullCycles++;
        console.log(`🔁 Completed cycle #${fullCycles} through all 3 AD's. Waiting ${fmtTime(WAIT_AFTER_AD_CYCLE)} before retrying...`);
        await sleep(WAIT_AFTER_AD_CYCLE);
      } else {
        console.log(`⏳ Waiting ${fmtTime(WAIT_BETWEEN_ATTEMPTS)} before next attempt (AD: ${ADs[adIndex]})...`);
        await sleep(WAIT_BETWEEN_ATTEMPTS);
      }
    } else {
      console.log("🎉🎉🎉 LOOKS LIKE IT WORKED! Check your console — instance may have been created!");
      break;
    }
  }
})();