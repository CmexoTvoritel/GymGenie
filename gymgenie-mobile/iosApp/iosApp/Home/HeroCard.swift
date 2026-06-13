import SwiftUI

private let hcBlack = Color(red: 0.039, green: 0.039, blue: 0.039)
private let hcMuted = Color(red: 0.545, green: 0.545, blue: 0.573)
private let hcSoft = Color(red: 0.957, green: 0.957, blue: 0.965)
private let hcBorder = Color(red: 0.929, green: 0.929, blue: 0.937)
private let hcStatBg = Color(red: 0.973, green: 0.973, blue: 0.980)
private let hcCoral = Color(red: 1.0, green: 0.353, blue: 0.235)

private struct ProfileAvatar: View {
    let name: String
    let size: CGFloat

    var body: some View {
        let initials = name.trimmingCharacters(in: .whitespaces)
            .components(separatedBy: .whitespaces)
            .prefix(2)
            .compactMap { $0.first.map { String($0).uppercased() } }
            .joined()
        ZStack {
            Circle()
                .fill(LinearGradient(
                    colors: [hcCoral, Color(red: 1.0, green: 0.541, blue: 0.431)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ))
                .frame(width: size, height: size)
            Text(initials.isEmpty ? "?" : initials)
                .font(.system(size: size * 0.36, weight: .bold))
                .foregroundColor(.white)
        }
    }
}

struct HeroCard: View {
    let displayName: String
    let email: String
    let hasPro: Bool
    let weightKg: String
    let heightCm: String
    let ageYears: String

    var body: some View {
        VStack(spacing: 0) {
            ProfileAvatar(name: displayName, size: 76)
                .padding(.top, 22)

            Text(displayName.isEmpty ? "Пользователь" : displayName)
                .font(.system(size: 21, weight: .bold))
                .foregroundColor(hcBlack)
                .padding(.top, 12)

            if !email.isEmpty {
                Text(email)
                    .font(.system(size: 15))
                    .foregroundColor(hcMuted)
                    .padding(.top, 2)
            }

            if hasPro {
                HStack(spacing: 5) {
                    Image("ic_premium_badge")
                        .renderingMode(.template)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 14, height: 14)
                    Text("PRO MEMBER")
                        .font(.system(size: 13, weight: .bold))
                        .tracking(0.4)
                }
                .foregroundColor(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Capsule().fill(hcBlack))
                .padding(.top, 10)
            } else {
                Text("FREE")
                    .font(.system(size: 13, weight: .bold))
                    .tracking(0.4)
                    .foregroundColor(hcMuted)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Capsule().fill(hcSoft))
                    .padding(.top, 10)
            }

            HStack(spacing: 0) {
                HeroStatItem(value: weightKg, unit: "кг", label: "Вес")
                Rectangle().fill(hcBorder).frame(width: 1, height: 32)
                HeroStatItem(value: heightCm, unit: "см", label: "Рост")
                Rectangle().fill(hcBorder).frame(width: 1, height: 32)
                HeroStatItem(value: ageYears, unit: ageYears == "—" ? "" : "лет", label: "Возраст")
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(RoundedRectangle(cornerRadius: 14).fill(hcStatBg))
            .padding(.top, 18)
            .padding(.bottom, 22)
            .padding(.horizontal, 20)
        }
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(Color.white)
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(hcBorder, lineWidth: 1))
        )
    }
}

private struct HeroStatItem: View {
    let value: String
    let unit: String
    let label: String

    var body: some View {
        VStack(spacing: 2) {
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(value)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(hcBlack)
                if value != "—" && !unit.isEmpty {
                    Text(unit)
                        .font(.system(size: 11))
                        .foregroundColor(hcMuted)
                }
            }
            Text(label)
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(hcMuted)
        }
        .frame(maxWidth: .infinity)
    }
}
