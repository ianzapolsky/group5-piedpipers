# The Plan

### Phase I
- [x] Vertical sweep
- [x] Magnet handoff

### Phase II
- [x] Central-ish greedy magnet Edward
- [x] clean up pipers, all go for the nearest rat and return to magnet Edward

### Phase III
- [x] If < 7 rats remain, send magnet home
- [x] Each piper gets one rat, brings it back to the left side

# Optimizations
- [x] Based on percentage of dimension we can cover with our pipers, decide on the sweep
- [x] Calculate rats per square meter and use as factor for sweep decision
- [x] If remaining rats > npipers - 1, do greedy, otherwise assign magnet and individual piper > individual ra
- [x] Support ALL number of pipers, no more hanging on last rats.
- [x] Optimized closest rat logic using both actual location and projected location. Potentially move a bit slower but no more hesitation. 
